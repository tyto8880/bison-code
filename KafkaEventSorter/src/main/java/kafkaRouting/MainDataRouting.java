/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kafkaRouting;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;


public class MainDataRouting {

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-event-processing");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
//        props.put(StreamsConfig., Serdes.String().getClass());

        final StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> mainDataStream = builder.stream("all-event-data");

//        KStream<String, String>[] branches = mainDataStream.branch(
//                (key, value) -> key.equalsIgnoreCase("0"),
//                (key, value) -> key.equalsIgnoreCase("1"),
//                (key, value) -> key.equalsIgnoreCase("2"),
//                (key, value) -> key.equalsIgnoreCase("3"),
//                (key, value) -> key.equalsIgnoreCase("4"),
//                (key, value) -> key.equalsIgnoreCase("5"),
//                (key, value) -> key.equalsIgnoreCase("100"),
//                (kay, value) -> true
//        );

//        KStream<String, String> eventA = mainDataStream.filter((k, v) -> v.equalsIgnoreCase("0"));

//        builder.<String, String>stream("streams-plaintext-input")
////               .flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split("\\W+")))
////               .groupBy((key, value) -> value)
////               .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("counts-store"))
////               .toStream()
//               .to("streams-wordcount-output", Produced.with(Serdes.String(), Serdes.String()));

//        KStream<String, String> overflow = eventA;
//
/* ================================================================================================================== */
/*                                                QUERY PROCESSING LOGIC                                              */
/* ================================================================================================================== */

    // +++++++++++++++++++++++++++++++++++++++++++ Temporal Processing +++++++++++++++++++++++++++++++++++++++++++
        // Determine if event type A occurs 3 or more times within 1 second
        int window_size = 10;
        long threshold = 3;
        String eventA = "a";
        mainDataStream.filter((k, v) -> v.equalsIgnoreCase(eventA))
            .groupBy((key, value) -> value)
            .windowedBy(TimeWindows.of(Duration.ofSeconds(window_size)).advanceBy(Duration.ofSeconds(1)))
            .count()
            .toStream((k, v) -> k.key())
            .filter((k, v) -> v >= threshold)
            .to("temporal-events");

        // Determine if event type B occurs within 5 seconds of event A
        String eventB = "b";
        mainDataStream.filter((k, v) -> v.equalsIgnoreCase(eventA) || v.equalsIgnoreCase(eventB))
            .groupBy((k, v) -> k)
            // .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofSeconds(window_size)).advanceBy(Duration.ofSeconds(5)))
            .aggregate(() -> 0L,
                    (String key, String value, Long acc) -> {
                        System.out.println(key + ":" + value + " " + Long.toString(acc));
                        if (acc == 0L && value.equalsIgnoreCase(eventA)) {
                            System.out.println("1 " + key + ":" + value + " " + Long.toString(acc));
                            return -1L;
                        }
                        else if (acc == -1L && value.equalsIgnoreCase(eventB)) {
                            System.out.println("3" + key + ":" + value + " " + Long.toString(acc));
                            return 100L;
                        }
                        return acc;
                    },
                    // (String key, String value, Long acc) -> {
                //     System.out.println(key + ":" + value);
                //     if (acc == 0L && key.equalsIgnoreCase(eventA)) {
                //         System.out.println("1" + key + ":" + value + " " + Long.toString(acc));
                //         return -1L;
                //     }
                //     else if (acc == 0L && key.equalsIgnoreCase(eventB)) {
                //         System.out.println("2" + key + ":" + value + " " + Long.toString(acc));
                //         return -2L;
                //     }
                //     else if (acc == -1L && key.equalsIgnoreCase(eventB)) {
                //         System.out.println("3" + key + ":" + value + " " + Long.toString(acc));
                //         return 1L;
                //     }
                //     else if (acc == -2L && key.equalsIgnoreCase(eventA)) {
                //         System.out.println("4" + key + ":" + value + " " + Long.toString(acc));
                //         return 2L;
                //     }
                //     else {
                //         System.out.println("5" + key + ":" + value + " " + Long.toString(acc));
                //         return acc;
                //     }
                // },
                Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("time-windowed-aggregated-stream-store") /* state store name */
                    .withValueSerde(Serdes.Long()) /* serde for aggregate value */
            )
            .toStream((Windowed<String> k, Long v) -> k.key())
            .filter((String k, Long v) -> v > 0L)
            .to("temporal-events");


    // +++++++++++++++++++++++++++++++++++++++++++ Evaluation Processing +++++++++++++++++++++++++++++++++++++++++++
        // Detect if event values exceed the 'value_threshold'
        long value_threshold = 10;
        mainDataStream.filter((k, v) -> Long.parseLong(v) > value_threshold)
            .to("evaluation-events");


        // Detect if the average of the last 'n' values for Event F is less than y
//        String eventF = "f";
//        mainDataStream.filter((k, v) -> k.equalsIgnoreCase(eventF))
//            .groupBy((k, v) -> v)
//            .windowedBy(TimeWindows.of(Duration.ofSeconds(window_size)).advanceBy(Duration.ofSeconds(1)))

    // +++++++++++++++++++++++++++++++++++++++++++ Sequence Processing +++++++++++++++++++++++++++++++++++++++++++
        // Detect when events occur in the order {A, B, C}
        String eventC = "c";
        mainDataStream.filter((k, v) -> v.equalsIgnoreCase(eventA) || v.equalsIgnoreCase(eventB)
                                || v.equalsIgnoreCase(eventC))
            .groupBy((k, v) -> k)
            .windowedBy(TimeWindows.of(Duration.ofSeconds(window_size)).advanceBy(Duration.ofSeconds(10)))
            .aggregate(() -> 0L,
                    (String key, String value, Long acc) -> {
                        if (acc == 0L && value.equalsIgnoreCase(eventA)) {
                            System.out.println("Detected Event A: " + key + ":" + value);
                            return 1L;
                        }
                        else if (acc == 1L) {
                            if (value.equalsIgnoreCase(eventB)) {
                                System.out.println("Found Sequence A, B: " + key + ":" + value);
                                return 2L; }
                            else if (value.equalsIgnoreCase(eventC)) {
                                System.out.println("Found Sequence A, C: resetting flags.");
                                return 0L;
                            }
                            System.out.println("Found Sequence A, A: investigating next potential sequence.");
                            return 1L;
                        }
                        else if (acc == 2L) {
                            if (value.equalsIgnoreCase(eventC)) {
                                System.out.println("Found Sequence A, B, C! Final Event: " + key + ":" + value);
                                return 3L;
                            }
                            else if (value.equalsIgnoreCase(eventB)) {
                                System.out.println("Found Sequence A, B, B: resetting flags.");
                                return 0L;
                            }
                            System.out.println("Found Sequence A, B, A: investigating next potential sequence.");
                        }
                        return acc;
                    },
                    Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("time-windowed-aggregated-stream-store2") /* state store name */
                            .withValueSerde(Serdes.Long()) /* serde for aggregate value */
            )
            .toStream((Windowed<String> k, Long v) -> k.key())
            .filter((String k, Long v) -> v >= 3L)
            .to("sequence-events");


        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
}
