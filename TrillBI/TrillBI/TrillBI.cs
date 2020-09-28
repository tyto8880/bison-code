using System;
using System.Text;

// Trill + networking imports
using System.Reactive.Linq;
using Microsoft.StreamProcessing;
using System.Threading.Tasks;
using System.Reactive.Subjects;
using System.Threading;
using System.Reactive.Disposables;

namespace TrillBI {
    internal struct LocationData {
        //public LocationData(double latitude, double longitude, long time) {
        //    this.latitude = latitude;
        //    this.longitude = longitude;
        //    this.time = time;
        //}

        public DateTime StartTime;
        public double Latitude;
        public double Longitude;

        //public override string ToString() => "Location Data: " + Latitude + ", " + Longitude;
        public override string ToString() {
            return new { this.StartTime, this.Latitude, this.Longitude }.ToString();
        }
    }
    class TrillBI {
        public static void Main(string[] args) {
            // start networking
            string ip = "127.0.0.1";
            int port = 8000;

            IObservable<LocationData> source = Observable.Create<LocationData>(
                observer => {
                    ThreadedIngress ingress = new ThreadedIngress(observer, ip, port);
                    Thread ingressThread = new Thread(new ThreadStart(ingress.Ingress));
                    ingressThread.Start();
                    //ingressThread.Join();

                    //ThreadedIngress ingress = new ThreadedIngress(observer, new LocationData { Latitude = 1, Longitude = 1, StartTime = DateTime.Now });
                    //Thread ingressThread = new Thread(new ThreadStart(ingress.Ingress));
                    //ingressThread.Start();
                    //ingressThread.Join();

                    //observer.OnCompleted();
                    return Disposable.Create(() => Console.WriteLine("Unsubscribed"));
                });
            //IObservable<PerformanceCounterSample> source = Observable.Create<PerformanceCounterSample>(
            //    observer => {
            //        ThreadedIngressDemo ingress = new ThreadedIngressDemo(observer);
            //        Thread AddToObserver = new Thread(new ThreadStart(ingress.Sample));
            //        AddToObserver.Start();

            //        //observer.OnNext(new PerformanceCounterSample { StartTime = startTime, Value = 2 });
            //        //observer.OnNext(new PerformanceCounterSample { StartTime = startTime, Value = 3 });
            //        return Disposable.Create(() => Console.WriteLine("Unsubscribed"));
            //    });

            var inputStream = 
                source.Select(e => StreamEvent.CreateStart(e.StartTime.Ticks, e))
                .ToStreamable(null,
                            FlushPolicy.FlushOnPunctuation,
                            PeriodicPunctuationPolicy.Time((ulong)TimeSpan.FromSeconds(1).Ticks));

            //var query1 = inputStream.Where(e => e.longitude == 40 || e.longitude == 50);
            inputStream.ToStreamEventObservable().ForEachAsync(m => WriteEvent(m)).Wait();

            Console.WriteLine("Done. Press ENTER to terminate");
            Console.ReadLine();
        }

        private static void WriteEvent<T>(StreamEvent<T> e) {
            if (e.IsData) {
                Console.WriteLine($"EventKind = {e.Kind,8}\t" +
                    $"StartTime = {new DateTime(e.StartTime)}\t" +
                    // "EndTime = {new DateTime(e.EndTime)}\t" +
                    $"Payload = ( {e.Payload} )");
            } else // IsPunctuation
              {
                Console.WriteLine($"EventKind = {e.Kind}\tSyncTime  = {new DateTime(e.StartTime)}");
            }
        }
    }
}
