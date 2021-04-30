using System;
using System.Collections.Generic;
using System.Text;
using System.Net;
using System.Net.Sockets;

using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.StreamProcessing;

namespace TrillBI {
    class ThreadedListener {
        private string ip;
        private int port;
        List<IObserver<StreamEvent<Payload>>> observers;

        public ThreadedListener(string ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        /* 
         * Mostly a normal blocking socket listener
         * calls observer.onNext() 
         */
        public void StartListener() {
            byte[] bytes;
            IPAddress ipAddress = IPAddress.Parse(ip);
            IPEndPoint localEndpoint = new IPEndPoint(ipAddress, port);
            int index = 0;
            observers = new List<IObserver<StreamEvent<Payload>>>();

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

                    string bytesString = null;
                    bytes = new byte[1024];

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
                    //Console.WriteLine("Text received : {0}", bytesString);
                    //observer.OnNext(ParseInput(bytesString, index));
                    foreach (IObserver<StreamEvent<Payload>> observer in observers) {
                        Payload payload = ParseInput(bytesString);
                        observer.OnNext(StreamEvent.CreatePoint(payload.Timestamp.Ticks, payload));
                        observer.OnNext(StreamEvent.CreatePunctuation<Payload>(payload.Timestamp.Ticks + TimeSpan.FromSeconds(0.5).Ticks));
                    }

                    index += 1;
                }
            } catch (Exception e) {
                Console.WriteLine(e.ToString());
            }
        }

        public void AddObserver(IObserver<StreamEvent<Payload>> observer) {
            observers.Add(observer);
        }

        private static Payload ParseInput(string input) {
            /*
             * Deserialize behavior:
             * - absent fields are null
             * - DateTimeOffset processed in ISO format retaining full precision
             * - 
             */
            return JsonSerializer.Deserialize<Payload>(input);
        }
    }
}
