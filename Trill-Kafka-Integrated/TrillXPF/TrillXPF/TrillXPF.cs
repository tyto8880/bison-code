using System;
using System.Threading;

using System.Reactive.Linq;
using System.Reactive.Disposables;
using Microsoft.StreamProcessing;

/*
 * Some notation:
 * examples from code are notated in backticks (` `)
 */

namespace TrillXPF {
    class TrillXPF {
        public static void Main(string[] args) {
            // start networking
            string ip = "127.0.0.1";
            int port = 9092;

            // We make a new thread dedicated to the Socket
            // detach from observer logic to provide multiple subscriptions
            ThreadedListener listener = new ThreadedListener(ip, port);
            Thread listenerThread = new Thread(new ThreadStart(listener.StartListener));
            listenerThread.Start();

            /*
             * source: Observable that adds an Observer to the listener when subscribed to, i.e. called ToObservable() on
             */
            IObservable<StreamEvent<Payload>> source = Observable.Create<StreamEvent<Payload>>(
                observer => {
                    listener.AddObserver(observer);
                    return Disposable.Create(() => Console.WriteLine("Unsubscribed"));
                });

            /*
             * inputStream: Streamable that ingresses from source
             * Uses a FlushPolicy and Punctuations, which inject empty events in order to force the stream to process
             * Flush policy does not take precedence over windows, i.e. HoppingWindows retain their time frame when queries are being run
             * DisorderPolicy specifies logic to follow when out-of-order events arrive; default behavior is to throw an exception
             */
            var inputStream = source.ToStreamable();

            // a sample query
            var query1 = inputStream.Where(e => e.EventID == 0);


            ///////////////////////////
            ///// BEGIN USE CASES /////
            ///////////////////////////


            /*
             * TEMPORAL CASES: use *temporal* stream
             * control window with temporalWindow
             */
            var temporalWindow = 8.0;
            var temporal = inputStream
                .HoppingWindowLifetime(TimeSpan.FromSeconds(temporalWindow).Ticks, TimeSpan.FromSeconds(1.0).Ticks);

            /*
             * 1: Detect when event B occurs within x minutes of event A 
             */
            var temporalA = temporal.Where(e => e.EventID == 0);
            var temporalB = temporal.Where(e => e.EventID == 1);
            // join currently matches on IsTemporal, which is always null...
            var temporal1 = temporalA.Join(temporalB, e => e.IsTemporal, e => e.IsTemporal,
                (left, right) => new {
                    leftID = left.DeviceID,
                    rightID = right.DeviceID,
                    leftTS = left.Timestamp,
                    rightTS = left.Timestamp,
                    leftEventID = left.EventID,
                    rightEventID = right.EventID
                })
                .Select(e => new { e.leftID, e.rightID, e.leftEventID, e.rightEventID });

            // Linq-syntax analog of the above
            var temporal1Linq = from left in temporalA
                                    join right in temporalB on
                                    left.IsTemporal equals right.IsTemporal
                                    select new {
                                        leftID = left.DeviceID,
                                        rightID = right.DeviceID,
                                        leftTS = left.Timestamp,
                                        rightTS = left.Timestamp,
                                        leftEventID = left.EventID,
                                        rightEventID = right.EventID
                                    };

            /*
             * 2: Detect when event A occurs at least n times within x minutes
             */
            // reuse the temporalA above
            var temporal2 = temporalA.Count();

            ////////////////////////////

            /*
             * SPATIAL CASES: use *spatial* stream
             * control window with spatialWindow
             */
            var spatialWindow = 30.0;
            var spatial = inputStream
                .HoppingWindowLifetime(TimeSpan.FromSeconds(spatialWindow).Ticks,
                TimeSpan.FromSeconds(1.0).Ticks);

            /*
             * 1: Detect two objects within x feet of each other
             */
            double spatialThreshold = 15.0;
            var spatialLat = spatial.Select(payload => new { payload.DeviceID, payload.EventData.latitude });
            var spatialLong = spatial.Select(payload => new { payload.DeviceID, payload.EventData.longitude });

            var latitudes = spatialLat
                .Join(
                spatialLat,
                (left, right) => new { leftID = left.DeviceID, rightID = right.DeviceID, difference = Math.Abs(left.latitude - right.latitude) })
                .Where(e => (e.leftID != e.rightID) && (e.difference <= spatialThreshold));

            var longitudes = spatialLong
                .Join(
                spatialLong,
                (left, right) => new { leftID = left.DeviceID, rightID = right.DeviceID, difference = Math.Abs(left.longitude - right.longitude) })
                .Where(e => (e.leftID != e.rightID) && (e.difference <= spatialThreshold));

            var spatial1 = from e1 in latitudes
                           join e2 in longitudes
                           on new { e1.leftID, e1.rightID } equals new { e2.leftID, e2.rightID }
                           select new {
                               e1.leftID,
                               e1.rightID,
                               latDiff = e1.difference,
                               longDiff = e2.difference
                           };

            ////////////////////////////

            /*
             * SEQUENTIAL CASES
             * control window with seqWindow
             */

            var seqWindow = 60.0;
            var sequence = inputStream;

            /*
             * 1: Detect when event A occurs, followed by event B, followed by event C
             */
            var sequence1 = sequence.FollowedByImmediate(
                (event1) => event1.EventID == 0,
                (event2) => event2.EventID == 1,
                (first, second) => second,
                TimeSpan.FromSeconds(seqWindow).Ticks)
                .FollowedByImmediate(
                sequence,
                (event2) => event2.EventID == 1,
                (event3) => event3.EventID == 2,
                (second, third) => third,
                TimeSpan.FromSeconds(seqWindow).Ticks);

            /*
             * 2: Detect when event A occurs, followed by event B or event C, followed by event D
             */
            var sequence2 = sequence.FollowedByImmediate(
                (event1) => event1.EventID == 0,
                (event2) => event2.EventID == 1 || event2.EventID == 2,
                (first, second) => second,
                TimeSpan.FromSeconds(seqWindow).Ticks)
                .FollowedByImmediate(
                sequence,
                (event2) => true,
                (event3) => event3.EventID == 3,
                (second, third) => third,
                TimeSpan.FromSeconds(seqWindow).Ticks);

            ////////////////////////////

            /*
             * EVALUATION CASES
             * value to compare is evalThreshold
             * control window with evalWindow
             */
            var evalThreshold = 20;
            var evalWindow = 10.0;
            var evaluation = inputStream
                .HoppingWindowLifetime(TimeSpan.FromSeconds(evalWindow).Ticks, TimeSpan.FromSeconds(1.0).Ticks);

            //var evaluation1 = evaluation.Where(e => Int32.Parse(e.EventData) > evalThreshold);
            
            
            ///////////////////////////
            ////// END USE CASES //////
            ///////////////////////////
            

            /*
             * Egress: each stream query is egressed to an Observable
             * Objects in the Observable are written via the arrow function (`() => new { }`)
             * Constructor parameters (`(start, end, payload)`) determine the data to pull from the query
             * action code goes in ForEachAsync block
             */
            inputStream.ToStreamEventObservable().Where(e => e.IsData).ForEachAsync(m => Console.WriteLine("Events detected! " + m.Payload.EventData));

            // Temporal output
            //temporal1.ToStreamEventObservable().ToStreamEventObservable().Where(e => e.IsData).ForEachAsync(m => Console.WriteLine("Events detected! " + m));
            //temporal2.ToStreamEventObservable()
            //    .Where(e => e.IsData)
            //    .ForEachAsync(m => {
            //        Console.WriteLine("Number of event A detected: " + m.Payload);
            //        // do things with m here...
            //        ulong n = 4;
            //        if (m.Payload >= n) {
            //            Console.WriteLine("More than n event A detected!");
            //        }
            //    });

            // Spatial output
            //spatialLat.ToStreamEventObservable().Where(e => e.IsData).ForEachAsync(m => Console.WriteLine("Events " + m));
            spatial1.ToStreamEventObservable().Where(e => e.IsData).ForEachAsync(m => Console.WriteLine("Devices " + m.Payload.leftID + " and " + m.Payload.rightID + " have a lat/long diff of (" + m.Payload.latDiff + ", " + m.Payload.longDiff + ")"));

            // Sequence output
            //sequence1.ToStreamEventObservable().Where(e => e.IsData).ForEachAsync(m => Console.WriteLine(m.StartTime + ", " + m.EndTime));
            //sequence2.ToStreamEventObservable().Where(e => e.IsData).ForEachAsync(m => Console.WriteLine(m));

            // Evaluation output
            //evaluation1.ToTemporalObservable((start, end, payload) => new { payload.DeviceID, payload.EventID }).ForEachAsync(m => Console.WriteLine(m));
        }
    }
}