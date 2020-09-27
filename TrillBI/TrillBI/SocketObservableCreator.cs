using Microsoft.StreamProcessing;
using System;
using System.Net;
using System.Net.Sockets;
using System.Collections.Generic;
using System.Linq;
using System.Reactive.Disposables;
using System.Reactive.Linq;
using System.Reactive.Subjects;
using System.Text;
using System.Threading.Tasks;

namespace TrillBI {
    class SocketObservableMaker {
        //private IObservable<LocationData> data;
        private string ip;
        private int port;

        public SocketObservableMaker(string ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        private static async Task StartListener(string ip, int port, IObserver<LocationData> observer) {
            IPAddress ipAddress = IPAddress.Parse(ip);
            IPEndPoint localEndpoint = new IPEndPoint(ipAddress, port);
            int index = 0;

            TcpListener server = new TcpListener(localEndpoint);
            Console.WriteLine("(Connect to " + localEndpoint.ToString() + " with client)");

            try {
                server.Start();
                await HandleClient(server, observer);
                //Thread.Sleep(6000);
            }
            finally {
                server.Stop();
            }
        }

        private static async Task HandleClient(TcpListener server, IObserver<LocationData> observer) {
            byte[] bytes;
            int bytesRead;
            string bytesString;
            int index = 0;

            bytes = new byte[1024];
            bytesString = null;
            TcpClient client = await server.AcceptTcpClientAsync();

            var stream = client.GetStream();
            bytesRead = await stream.ReadAsync(bytes, 0, bytes.Length);
            bytesString += Encoding.ASCII.GetString(bytes, 0, bytesRead);
            Console.WriteLine("Bytes rec'd: {0}\tData so far : {1}", bytesRead, bytesString);
            observer.OnNext(ParseInput(bytesString, index));
            observer.OnCompleted();
            index += 1;
        }

        public IObservable<LocationData> CreateObservable() {
            var data = Observable.Create<LocationData>(
                async observer => {
                    await StartListener(ip, port, observer);
                    //observer.OnCompleted();
                    return Disposable.Create(() => Console.WriteLine("Unsubscribed"));
                });

            return data;
        }

        public static async Task eMain() {
            string ip = "127.0.0.1";
            int port = 8000;

            IObservable<LocationData> data = Observable.Create<LocationData>(
                async observer => {
                    await StartListener(ip, port, observer);
                    //observer.OnCompleted();
                    return Disposable.Create(() => Console.WriteLine("Unsubscribed"));
                });

            var inputStream = data.Select(r => {
                //Console.WriteLine(r);
                return StreamEvent.CreateStart(r.time, r);
            }).ToStreamable();

            var query1 = inputStream.Where(e => e.longitude == 40 || e.longitude == 50);
            await inputStream.ToStreamEventObservable().ForEachAsync(m => Console.WriteLine(m));

            Console.WriteLine("Done. Press ENTER to terminate");
            Console.ReadLine();
        }

        private static LocationData ParseInput(string input, long time) {
            string[] values = input.Split(new string[] { ", " }, StringSplitOptions.None);
            //Console.WriteLine(values[0] + ", " + values[1]);
            return new LocationData(Convert.ToDouble(values[0]), Convert.ToDouble(values[1]), time);
        }
    }
}
