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
        // WE ABSOLUTELY NEED A DateTime OBJECT HERE AT LEAST FOR RIGHT NOW unless i'm like megabad
        public LocationData(DateTime StartTime, double Latitude, double Longitude) {
            this.Latitude = Latitude;
            this.Longitude = Longitude;
            this.StartTime = StartTime;
        }

        public DateTime StartTime;
        public double Latitude;
        public double Longitude;

        public override string ToString() => "Latitude: " + Latitude + ", Longitude: " + Longitude;
        //public override string ToString() {
        //    return new { this.StartTime, this.Latitude, this.Longitude }.ToString();
        //}
    }
    class TrillBI {
        public static void Main(string[] args) {
            // start networking
            string ip = "127.0.0.1";
            int port = 8000;

            /*
             * source: Observable that starts a new ThreadedListener when subscribed to, i.e. called ToStreamEventObservable() on
             * We make a new thread dedicated to the Socket
             */
            IObservable<LocationData> source = Observable.Create<LocationData>(
                observer => {
                    ThreadedListener listener = new ThreadedListener(ip, port, observer);
                    Thread listenerThread = new Thread(new ThreadStart(listener.StartListener));
                    listenerThread.Start();

                    return Disposable.Create(() => Console.WriteLine("Unsubscribed"));
                });

            //source.Subscribe(e => Console.WriteLine(e));

            /*
             * inputStream: Streamable that ingresses from source
             * Uses a FlushPolicy and Punctuations, which inject empty events in order to force the stream to process
             */
            var inputStream = 
                source.Select(e => StreamEvent.CreateStart(e.StartTime.Ticks, e))
                .ToStreamable(null,
                            FlushPolicy.FlushOnPunctuation,
                            PeriodicPunctuationPolicy.Time((ulong)TimeSpan.FromSeconds(1).Ticks));

            // a sample query
            var query1 = inputStream.Where(e => e.Latitude == 40.12071);

            //inputStream.ToStreamEventObservable().ForEachAsync(m => WriteEvent(m));
            inputStream.ToStreamEventObservable().ForEachAsync(m => WriteEvent(m));

            //Console.WriteLine("Done. Press ENTER to terminate");
            //Console.ReadLine();
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
