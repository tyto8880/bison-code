using System;
using System.Linq;
using System.Reactive.Linq;
using Microsoft.StreamProcessing;
using System.Diagnostics;
using System.Threading;
using System.Reactive.Subjects;

namespace TrillBI {
    /// <summary>
    /// Struct that holds the actual performance counter
    /// </summary>
    internal struct PerformanceCounterSample {
        public DateTime StartTime;
        public float Value;

        public override string ToString() {
            return new { this.StartTime, this.Value }.ToString();
        }
    }

    /// <summary>
    /// An observable source based on a local machine performance counter.
    /// </summary>
    internal sealed class PerformanceCounterObservable : IObservable<PerformanceCounterSample> {
        private readonly Func<PerformanceCounter> createCounter;
        private readonly TimeSpan pollingInterval;

        public PerformanceCounterObservable(
            string categoryName,
            string counterName,
            string instanceName,
            TimeSpan pollingInterval) {
            // create a new performance counter for every subscription
            this.createCounter = () => new PerformanceCounter(categoryName, counterName, instanceName, true);
            this.pollingInterval = pollingInterval;
        }

        public IDisposable Subscribe(IObserver<PerformanceCounterSample> observer) {
            return new Subscription(this, observer);
        }

        private sealed class Subscription : IDisposable {
            private readonly PerformanceCounter counter;
            private readonly TimeSpan pollingInterval;
            private readonly IObserver<PerformanceCounterSample> observer;
            private readonly Timer timer;
            private readonly object sync = new object();
            private CounterSample previousSample;
            private bool isDisposed;

            public Subscription(PerformanceCounterObservable observable, IObserver<PerformanceCounterSample> observer) {
                // create a new counter for this subscription
                this.counter = observable.createCounter();
                this.pollingInterval = observable.pollingInterval;
                this.observer = observer;

                // seed previous sample to support computation
                this.previousSample = this.counter.NextSample();

                // create a timer to support polling counter at an interval
                this.timer = new Timer(Sample);
                this.timer.Change(this.pollingInterval.Milliseconds, -1);
            }

            private void Sample(object state) {
                lock (this.sync) {
                    if (!this.isDisposed) {
                        var startTime = DateTime.Now;
                        CounterSample currentSample = this.counter.NextSample();
                        float value = CounterSample.Calculate(this.previousSample, currentSample);
                        this.observer.OnNext(new PerformanceCounterSample { StartTime = startTime, Value = value });
                        this.previousSample = currentSample;
                        this.timer.Change(this.pollingInterval.Milliseconds, -1);
                    }
                }
            }

            public void Dispose() {
                lock (this.sync) {
                    if (!this.isDisposed) {
                        this.isDisposed = true;
                        this.timer.Dispose();
                        this.counter.Dispose();
                    }
                }
            }
        }
    }

    class Demo {
        public static void sMain(string[] args) {
            // Poll performance counter 4 times a second
            var pollingInterval = TimeSpan.FromSeconds(.25);

            // Take the total processor utilization performance counter
            string categoryName = "Processor";
            string counterName = "% Processor Time";
            string instanceName = "_Total";

            // Create an observable that feeds the performance counter periodically
            IObservable<PerformanceCounterSample> source =
                new PerformanceCounterObservable(categoryName, counterName, instanceName, pollingInterval);

            // Load the observable as a stream in Trill, injecting a punctuation every second. Because we use
            // FlushPolicy.FlushOnPunctuation, this will also flush the data every second.
            var inputStream =
                source.Select(e => StreamEvent.CreateStart(e.StartTime.Ticks, e))
                .ToStreamable(
                    null,
                    FlushPolicy.FlushOnPunctuation,
                    PeriodicPunctuationPolicy.Time((ulong)TimeSpan.FromSeconds(1).Ticks));

            // Query 1: Aggregate query
            long windowSize = TimeSpan.FromSeconds(2).Ticks;
            var query1 = inputStream.TumblingWindowLifetime(windowSize).Average(e => e.Value);

            //// Query 2: look for pattern of [CPU < 10] --> [CPU >= 10]
            //var query2 = inputStream
            //    .AlterEventDuration(TimeSpan.FromSeconds(10).Ticks)
            //    .Detect(default(Tuple<float, float>), // register to store CPU value
            //        p => p
            //        .SingleElement(e => e.Value < 10, (ts, ev, reg) => new Tuple<float, float>(ev.Value, 0))
            //        .SingleElement(e => e.Value >= 10, (ts, ev, reg) => new Tuple<float, float>(reg.Item1, ev.Value)));

            //// Query 3: look for pattern of [k increasing CPU values --> drop CPU], report max, k
            //int k = 2;
            //var query3 = inputStream
            //    .AlterEventDuration(TimeSpan.FromSeconds(10).Ticks)
            //    .Detect(default(Tuple<float, int>), // register to store CPU value, incr count
            //        p => p
            //        .KleenePlus(e =>
            //        e.SingleElement((ev, reg) => reg == null || ev.Value > reg.Item1, (ts, ev, reg) => new Tuple<float, int>(ev.Value, reg == null ? 1 : reg.Item2 + 1)))
            //        .SingleElement((ev, reg) => ev.Value < reg.Item1 && reg.Item2 > k, (ts, ev, reg) => reg),
            //        allowOverlappingInstances: false);

            // Egress results and write to console
            query1.ToStreamEventObservable().ForEachAsync(e => WriteEvent(e)).Wait();
        }

        private static void WriteEvent<T>(StreamEvent<T> e) {
            if (e.IsData) {
                Console.WriteLine($"EventKind = {e.Kind,8}\t" +
                    $"StartTime = {new DateTime(e.StartTime)}\t" +
                    // "EndTime = {new DateTime(e.EndTime)}\t" +
                    $"Payload = ( {e.Payload.ToString()} )");
            } else // IsPunctuation
              {
                Console.WriteLine($"EventKind = {e.Kind}\tSyncTime  = {new DateTime(e.StartTime)}");
            }
        }
    }
}
