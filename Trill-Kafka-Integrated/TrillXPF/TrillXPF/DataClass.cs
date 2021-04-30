using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace TrillXPF {
    public struct Location {
        public float latitude;
        public float longitude;
        public int accuracy;

        public Location(float latitude, float longitude, int accuracy) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
        }

        public override string ToString() => "Latitude: " + latitude + ", Longitude: " + longitude;
        //public override string ToString() {
        //    return new { this.StartTime, this.Latitude, this.Longitude }.ToString();
        //}
    }

    public class Payload {
        public Payload(int DeviceID, long Timestamp, short EventID, Location EventData) {
            this.DeviceID = DeviceID;
            this.Timestamp = DateTimeOffset.FromUnixTimeSeconds(Timestamp);
            this.EventID = EventID;
            this.EventData = EventData;
            this.IsTemporal = true;
        }

        public int DeviceID { get; set; }
        public DateTimeOffset Timestamp { get; set; }
        public short EventID { get; set; }
        public Location EventData { get; set; }
        public bool IsTemporal { get; set; }
    }
}