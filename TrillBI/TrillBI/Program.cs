using System;
using System.Net;
using System.Net.Sockets;
using System.Text;

// Trill + networking imports
using RxSockets;
using System.Reactive.Linq;
using Microsoft.StreamProcessing;
using System.Threading.Tasks;

namespace TrillBI {
    class Program {
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
        private static LocationData[] data = new LocationData[3];

        static async Task Main(string[] args) {
            // start networking
            String ip = "127.0.0.1";
            int port = 8000;

            // maybe move to new method

            // make incoming data observable
            IObservable<LocationData> inputObservable = data.ToObservable();
            IStreamable<Empty, LocationData> inputStream;

            StartListener(ip, port);
            inputStream = inputObservable.Select(r => StreamEvent.CreateInterval(r.time, r.time + 1, r)).ToStreamable();
            inputStream.ToStreamEventObservable().ForEachAsync(m => Console.WriteLine(m)).Wait();

            Console.WriteLine("Done. Press ENTER to terminate");
            Console.ReadLine();
        }

        static void StartListener(String ip, int port) {
            byte[] bytes = new byte[1024];
            IPAddress ipAddress = IPAddress.Parse(ip);
            IPEndPoint localEndpoint = new IPEndPoint(ipAddress, port);
            int index = 0;

            // make non-blocking at some point
            Socket server = new Socket(ipAddress.AddressFamily, SocketType.Stream, ProtocolType.Tcp);
            //server.Blocking = false;

            Console.WriteLine(localEndpoint.ToString());

            //IRxSocketServer server = RxSocketServer.CreateOnEndPoint(localEndpoint);
            //server.AcceptObservable.Subscribe(onNext: acceptClient =>
            //{
            //    // prints out characters received
            //    // want to put into byte array eventually though, so...
            //    //acceptClient.ReceiveObservable.ForEachAsync(m => Console.Write(Convert.ToChar(m)));

            //    // let class var data hold the Observable containing bytes
            //    data = acceptClient.ReceiveObservable;
            //});

            //await Task.Delay(3000);

            //// Disconnect.
            //await server.DisposeAsync();

            try {
                server.Bind(localEndpoint);
                server.Listen(10);

                while (true) {
                    Console.WriteLine("Waiting for a connection...");
                    // Program is suspended while waiting for an incoming connection.  
                    Socket handler = server.Accept();
                    handler.Blocking = false;
                    string socketdata = null;

                    while (true) {
                        int bytesRec = handler.Receive(bytes);
                        if (bytesRec > 0) {
                            socketdata += Encoding.ASCII.GetString(bytes, 0, bytesRec);
                            Console.WriteLine("Bytes rec'd: {0}\tData so far : {1}", bytesRec, socketdata);
                        } else {
                            break;
                        }
                    }

                    // Show the data on the console.  
                    Console.WriteLine("Text received : {0}", socketdata);
                    data[index] = ParseInput(socketdata, index);
                    index += 1;
                    if (index == 3) {
                        break;
                    }
                }
            } catch (Exception e) {
                Console.WriteLine(e.ToString());
            }
        }

        private static LocationData ParseInput(string input, long time) {
            string[] values = input.Split(new string[] { ", " }, StringSplitOptions.None);
            //Console.WriteLine(values[0] + ", " + values[1]);
            return new LocationData(Convert.ToDouble(values[0]), Convert.ToDouble(values[1]), time);
        }
    }
}
