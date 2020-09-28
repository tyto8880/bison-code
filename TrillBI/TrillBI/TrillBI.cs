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
                    ThreadedListener listener = new ThreadedListener(ip, port, observer);
                    Thread listenerThread = new Thread(new ThreadStart(listener.StartListener));
                    listenerThread.Start();

                    return Disposable.Create(() => Console.WriteLine("Unsubscribed"));
                });

            var inputStream = 
                source.Select(e => StreamEvent.CreateStart(e.StartTime.Ticks, e))
                .ToStreamable(null,
                            FlushPolicy.FlushOnPunctuation,
                            PeriodicPunctuationPolicy.Time((ulong)TimeSpan.FromSeconds(1).Ticks));

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
