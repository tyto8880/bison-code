using System;
using System.Threading;
using System.Collections.Generic;

using Microsoft.StreamProcessing;
using Confluent.Kafka;
using Confluent.Kafka.SyncOverAsync;
using Confluent.SchemaRegistry;
using Confluent.SchemaRegistry.Serdes;

namespace TrillXPF {
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
            string[] topics = {
                "all-event-data-test"
            };

            var consumerConfig = new ConsumerConfig {
                BootstrapServers = ip + ":" + port.ToString(),
                EnableAutoCommit = false,
                GroupId = "1"
            };

            var schemaRegistryConfig = new SchemaRegistryConfig {
                Url = "localhost:8081"
            };
            
            const int commitPeriod = 5;

            observers = new List<IObserver<StreamEvent<Payload>>>();

            CancellationTokenSource cts = new CancellationTokenSource();
            using (var schemaRegistry = new CachedSchemaRegistryClient(schemaRegistryConfig))
            using (var consumer = new ConsumerBuilder<Ignore, EventRecord>(consumerConfig)
                .SetValueDeserializer(new AvroDeserializer<EventRecord>(schemaRegistry).AsSyncOverAsync())
                .SetErrorHandler((_, e) => Console.WriteLine($"Error: {e.Reason}"))
                .Build()) {
                consumer.Subscribe(topics);

                try {
                    while (true) {
                        try {
                            var consumeResult = consumer.Consume(cts.Token);
                            var message = consumeResult.Message.Value;
                            foreach (IObserver<StreamEvent<Payload>> observer in observers) {
                                Payload payload = new Payload(message.DeviceID, message.Timestamp, (short) message.EventID, new Location(message.EventData.Latitude, message.EventData.Longitude, message.EventData.Accuracy));
                                observer.OnNext(StreamEvent.CreatePoint(payload.Timestamp.Ticks, payload));
                                observer.OnNext(StreamEvent.CreatePunctuation<Payload>(payload.Timestamp.Ticks + 2));
                            }

                            if (consumeResult.Offset % commitPeriod == 0) {
                                try {
                                    consumer.Commit(consumeResult);
                                } catch (KafkaException e) {
                                    Console.WriteLine($"Commit error: {e.Error.Reason}");
                                }
                            }
                        } catch (ConsumeException e) {
                            Console.WriteLine($"Consume error: {e.Error.Reason}");
                        }
                    }
                } catch (OperationCanceledException e) {
                    consumer.Close();
                }
            }

            cts.Cancel();
        }

        public void AddObserver(IObserver<StreamEvent<Payload>> observer) {
            observers.Add(observer);
        }
    }
}