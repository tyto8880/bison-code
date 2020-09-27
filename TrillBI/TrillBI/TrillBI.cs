using System;
using System.Text;

// Trill + networking imports
using System.Reactive.Linq;
using Microsoft.StreamProcessing;
using System.Threading.Tasks;
using System.Reactive.Subjects;

namespace TrillBI {
    struct LocationData {
        public LocationData(double latitude, double longitude, long time) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.time = time;
        }

        public double latitude;
        public double longitude;
        public long time;

        public override string ToString() => "Location Data: " + latitude + ", " + longitude;
    }
    class TrillBI {
        private struct LocationData {
            public LocationData(double latitude, double longitude, long time) {
                this.latitude = latitude;
                this.longitude = longitude;
                this.time = time;
            }

            public double latitude;
            public double longitude;
            public long time;

            public override string ToString() => "Location Data: " + latitude + ", " + longitude;
        }

        // Incoming data from the client.
        private static Subject<LocationData> data;

        static async Task Maisn(string[] args) {
            data = new Subject<LocationData>();

            //data.Subscribe(x => Console.WriteLine("Value published: {0}", x));
            data.OnNext(new LocationData(40, 40, 1));

            // start networking
            string ip = "127.0.0.1";
            int port = 8000;

            // make incoming data observable
            //IObservable<LocationData> inputObservable = data;
            IStreamable<Empty, LocationData> inputStream;

            inputStream = data.Select(r => StreamEvent.CreateInterval(r.time, r.time + 1, r)).ToStreamable();
            inputStream.ToStreamEventObservable().ForEachAsync(m => WriteEvent(m));
            Console.WriteLine("here");

            //await StartListener(ip, port);

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
