using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace TrillBI {
    public struct Location {
        public double latitude;
        public double longitude;
        public byte accuracy;

        public Location(string location) {
            var locElements = location.Split(' ');

            this.latitude = Double.Parse(locElements[0]);
            this.longitude = Double.Parse(locElements[1]);
            this.accuracy = Byte.Parse(locElements[2]);
        }

        public override string ToString() => "Latitude: " + latitude + ", Longitude: " + longitude;
        //public override string ToString() {
        //    return new { this.StartTime, this.Latitude, this.Longitude }.ToString();
        //}
    }
    public class Payload {
        public int DeviceID { get; set; }
        public DateTimeOffset Timestamp { get; set; }
        public short EventID { get; set; }
        public string EventData { get; set; }
        public bool IsTemporal { get; set; }
    }
}
