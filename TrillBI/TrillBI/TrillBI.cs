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
        static async Task Main(string[] args) {
            IObservable<LocationData> data;
            
            // start networking
            string ip = "127.0.0.1";
            int port = 8000;
            SocketObservableCreator creator = new SocketObservableCreator(ip, port);

            data = creator.CreateObservable();

            // make incoming data observable
            //IObservable<LocationData> inputObservable = data;

            var inputStream = data.Select(r => {
                //Console.WriteLine(r);
                return StreamEvent.CreateStart(r.time, r);
            }).ToStreamable();

            var query1 = inputStream.Where(e => e.longitude == 40 || e.longitude == 50);
            await inputStream.ToStreamEventObservable().ForEachAsync(m => Console.WriteLine(m));

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
