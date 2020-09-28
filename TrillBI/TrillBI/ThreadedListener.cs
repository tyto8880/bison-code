using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;

namespace TrillBI {
    //class ThreadedIngress {
    //    private IObserver<LocationData> observer;
    //    private string ip;
    //    private int port;
    //    public ThreadedIngress(IObserver<LocationData> observer, string ip, int port) {
    //        this.observer = observer;
    //        this.ip = ip;
    //        this.port = port;
    //    }
    //    public void Ingress() {
    //        //ThreadedListener listener = new ThreadedListener(ip, port, observer);
    //        //Thread listenerThread = new Thread(new ThreadStart(listener.StartListener));
    //        //listenerThread.Start();
    //    }
    //}
    class ThreadedListener {
        private string ip;
        private int port;
        private IObserver<LocationData> observer;

        public ThreadedListener(string ip, int port, IObserver<LocationData> observer) {
            this.ip = ip;
            this.port = port;
            this.observer = observer;
        }

        public void StartListener() {
            byte[] bytes;
            IPAddress ipAddress = IPAddress.Parse(ip);
            IPEndPoint localEndpoint = new IPEndPoint(ipAddress, port);
            int index = 0;

            // make blocking socket server
            Socket server = new Socket(ipAddress.AddressFamily, SocketType.Stream, ProtocolType.Tcp);

            Console.WriteLine(localEndpoint.ToString());

            try {
                server.Bind(localEndpoint);
                server.Listen(10);

                while (true) {
                    Console.WriteLine("Waiting for a connection...");
                    // Program is suspended while waiting for an incoming connection.  
                    Socket handler = server.Accept();

                    bytes = new byte[1024];
                    string bytesString = null;

                    while (true) {
                        int bytesRec = handler.Receive(bytes);
                        if (bytesRec > 0) {
                            bytesString += Encoding.ASCII.GetString(bytes, 0, bytesRec);
                            //Console.WriteLine("Bytes rec'd: {0}\tData so far : {1}", bytesRec, bytesString);
                        } else {
                            break;
                        }
                    }

                    // Show the data on the console.  
                    Console.WriteLine("Text received : {0}", bytesString);
                    observer.OnNext(ParseInput(bytesString, index));

                    // make IObserver ingress thread
                    //ThreadedIngress threadedIngress = new ThreadedIngress(observer, new LocationData { Latitude = 1, Longitude = 1, StartTime = DateTime.Now });
                    //Thread ingress = new Thread(new ThreadStart(threadedIngress.Ingress));
                    //ingress.Start();
                    //break;
                    index += 1;
                }
            } catch (Exception e) {
                Console.WriteLine(e.ToString());
            }
        }

        private static LocationData ParseInput(string input, long time) {
            string[] values = input.Split(new string[] { ", " }, StringSplitOptions.None);
            //Console.WriteLine(values[0] + ", " + values[1]);
            return new LocationData { Latitude = Convert.ToDouble(values[0]), Longitude = Convert.ToDouble(values[1]), StartTime = DateTime.Now };
        }
    }
}
