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

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * In this example, we implement a simple WordCount program using the high-level Streams DSL
 * that reads from a source topic "streams-plaintext-input", where the values of messages represent lines of text,
 * split each text line into words and then compute the word occurence histogram, write the continuous updated histogram
 * into a topic "streams-wordcount-output" where each record is an updated count of a single word.
 */
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
        long threshold = 3;
        mainDataStream.filter((k, v) -> k.equalsIgnoreCase("0"))
            .groupBy((key, value) -> value)
            .windowedBy(TimeWindows.of(Duration.ofSeconds(2)).advanceBy(Duration.ofSeconds(1)))
            .count()
            .toStream((k, v) -> k.key())
            .filter((k, v) -> v >= threshold)
            .to("query-output");

//            groupedStream.windowedBy(TimeWindows.of(Duration.ofSeconds(4)))
//                .count()
//                .toStream()
//                    .filter((k, v) -> )
        //, Produced.with(Serdes.serdeFrom(), Serdes.Long())

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
