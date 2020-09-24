using System;
using System.Net;
using System.Net.Sockets;
using System.Text;

// Trill + networking imports
using System.Reactive.Linq;
using Microsoft.StreamProcessing;

namespace TrillBI {
    class Program {
        
        // Incoming data from the client.
        public static string data = null;

        private struct LocationData {
            public LocationData(long latitude, long longitude) {
                this.latitude = latitude;
                this.longitude = longitude;
            }

            public long latitude;
            public long longitude;
        }
        static void Main(string[] args) {
            // start networking
            String ip = "127.0.0.1";
            int port = 8000;
            StartListener(ip, port);

            
        }

        static void StartListener(String ip, int port) {
            byte[] bytes = new byte[1024];
            IPAddress ipAddress = IPAddress.Parse(ip);
            IPEndPoint localEndpoint = new IPEndPoint(ipAddress, port);
            Socket server = new Socket(ipAddress.AddressFamily, SocketType.Stream, ProtocolType.Tcp);

            Console.WriteLine(localEndpoint.ToString());

            try {
                server.Bind(localEndpoint);
                server.Listen(10);
                
                while (true) {
                    Console.WriteLine("Waiting for a connection...");
                    // Program is suspended while waiting for an incoming connection.  
                    Socket handler = server.Accept();
                    data = null;

                    while (true) {
                        int bytesRec = handler.Receive(bytes);
                        data += Encoding.ASCII.GetString(bytes, 0, bytesRec);
                        if (bytesRec > 0) {
                            Console.WriteLine("Bytes rec'd: {0}\tData so far : {1}", bytesRec, data);
                        } else {
                            break;
                        }
                        if (data.IndexOf("<EOF>") > -1) {
                            break;
                        }
                    }

                    // Show the data on the console.  
                    Console.WriteLine("Text received : {0}", data);
                }
            } catch (Exception e) {
                Console.WriteLine(e.ToString());
            }
        }
    }
}
