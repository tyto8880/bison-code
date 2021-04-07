/*
 * This file defines a stream called mainDataStream that will hold all
 * event data passed into the system. It also defines all stream processing
 * that will be done on mainDataStream; and where the resultant records
 * will be routed.
 *
 * Developed 8/2020 - 4/2021 by Jared Keefer, Erik Rhodes, and Tyler Tokumoto
 * All rights reserved.
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


/* ================================================================================================================== */
/*                                                QUERY PROCESSING LOGIC                                              */
/* ================================================================================================================== */

// +++++++++++++++++++++++++++++++++++++++++++ Temporal Processing +++++++++++++++++++++++++++++++++++++++++++
        // Determine if event type A occurs 3 or more times within 1 second
        int window_size = 5;
        int advance_by = 1;
        long threshold = 3;
        String eventA = "a";
        mainDataStream.filter((k, v) -> v.equalsIgnoreCase(eventA))
            .groupBy((key, value) -> value)
            .windowedBy(TimeWindows.of(Duration.ofSeconds(window_size)).advanceBy(Duration.ofSeconds(advance_by)))
            .count()
            .toStream((k, v) -> k.key())
            .filter((k, v) -> v >= threshold)
            .to("temporal-events");

        // Determine if event type B occurs within 5 seconds of event A
        String eventB = "b";
        mainDataStream.filter((k, v) -> v.equalsIgnoreCase(eventA) || v.equalsIgnoreCase(eventB))
            .groupBy((k, v) -> "")
            .windowedBy(TimeWindows.of(Duration.ofSeconds(window_size)).advanceBy(Duration.ofSeconds(advance_by)))
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
                Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("time-windowed-aggregated-stream-store") /* state store name */
                    .withValueSerde(Serdes.Long()) /* serde for aggregate value */
            )
            .toStream((Windowed<String> k, Long v) -> k.key())
            .filter((String k, Long v) -> v > 0L)
            .to("temporal-events");


// +++++++++++++++++++++++++++++++++++++++++++ Evaluation Processing +++++++++++++++++++++++++++++++++++++++++++
        // Detect if event values exceed the 'value_threshold'
        long value_threshold = 10;
        mainDataStream.filter((k, v) -> {
            try {
                return Long.parseLong(v) > value_threshold;
            }
            catch(Exception e) {
                return false;
            }
        }).to("evaluation-events");

// +++++++++++++++++++++++++++++++++++++++++++ Sequence Processing +++++++++++++++++++++++++++++++++++++++++++
        // Detect when events occur in the order {A, B, C}
        String eventC = "c";
        String eventD = "d";
        mainDataStream.filter((k, v) -> v.equalsIgnoreCase(eventA) || v.equalsIgnoreCase(eventB)
                                || v.equalsIgnoreCase(eventC) || v.equalsIgnoreCase(eventD))
            .groupBy((k, v) -> "")
            .windowedBy(TimeWindows.of(Duration.ofSeconds(window_size)).advanceBy(Duration.ofSeconds(advance_by)))
            .aggregate(() -> 0L,
                    (String key, String value, Long acc) -> {
                        if (acc == 0L && value.equalsIgnoreCase(eventA)) {
                            System.out.println("Detected Event A: " + key + ":" + value);
                            return 1L;
                        }
                        else if (acc == 1L) {
                            if (value.equalsIgnoreCase(eventA)) {
                                System.out.println("Found Sequence A, A: resetting flags.");
                                return 1L;
                            }
                            else if (value.equalsIgnoreCase(eventB)) {
                                System.out.println("Found Sequence A, B: " + key + ":" + value);
                                return 2L;
                            }
                            else {
                                System.out.println("Found Sequence A, x: investigating next potential sequence.");
                                return 0L;
                            }
                        }
                        else if (acc == 2L) {
                            if (value.equalsIgnoreCase(eventC)) {
                                System.out.println("Found Sequence A, B, C! Final Event: " + key + ":" + value);
                                return 3L;
                            }
                            else if (value.equalsIgnoreCase(eventA)) {
                                System.out.println("Found Sequence A, B, A: investigating next potential sequence.");
                                return 1L;
                            }
                            else {
                                System.out.println("Found Sequence A, B, x: resetting flags.");
                                return 0L;
                            }
                        }
                        else if (acc == 3L) {
                            if (value.equalsIgnoreCase(eventD)) {
                                System.out.println("Found Sequence A, B, C, D! Final Event: " + key + ":" + value);
                                return 4L;
                            }
                            else if (value.equalsIgnoreCase(eventA)) {
                                System.out.println("Found Sequence A, B, C, A: investigating next potential sequence.");
                                return 1L;
                            }
                            else {
                                System.out.println("Found Sequence A, B, C, x: resetting flags.");
                                return 0L;
                            }
                        }
                        return acc;
                    },
                    Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("time-windowed-aggregated-stream-store2") /* state store name */
                            .withValueSerde(Serdes.Long()) /* serde for aggregate value */
            )
            .toStream((Windowed<String> k, Long v) -> k.key())
            .filter((String k, Long v) -> v >= 3L)
            .to("sequence-events");

// ++++++++++++++++++++++++++++++++++++++++++++ Geospatial Processing +++++++++++++++++++++++++++++++++++++++++++
        // Detect 2 objects within x feet of each other
            // NOTE: Query assumes that all geospatial payloads will be keyed with 'geo'
        Double d_thresh = 100.0;
        String d1 = "a";
        String d2 = "b";
        KTable<Windowed<String>, String> a_dev_data = mainDataStream.filter((k, v) -> k.equalsIgnoreCase("geo") &&
                                                    v.split("\\s+")[0].equalsIgnoreCase(d1))
                .groupBy((k, v) -> "")
                .windowedBy(TimeWindows.of(Duration.ofSeconds(window_size)).advanceBy(Duration.ofSeconds(advance_by)))
                .aggregate(() -> "",
                        ((k,v, acc) -> v)
                );

        KTable<Windowed<String>, String> b_dev_data = mainDataStream.filter((k, v) -> k.equalsIgnoreCase("geo") &&
                                                    v.split("\\s+")[0].equalsIgnoreCase(d2))
                .groupBy((k, v) -> "")
                .windowedBy(TimeWindows.of(Duration.ofSeconds(window_size)).advanceBy(Duration.ofSeconds(advance_by)))
                .aggregate(() -> "",
                        ((k,v, acc) -> v)
                );

        a_dev_data.join(b_dev_data,
                (v1, v2) -> {
                    //TESTING
                    System.out.println(v1);
                    System.out.println(v2);

                    String[] a_separator = v1.split("\\s+");
                    System.out.println("Split successful");
                    Double a_lat = Double.parseDouble(a_separator[1]);
                    System.out.println("index 1 fine, a_lat: " + Double.toString(a_lat));
                    Double a_lon = Double.parseDouble(a_separator[2]);
                    System.out.println("index 2 fine, a_lon: " + Double.toString(a_lon));

                    String[] b_separator = v2.split("\\s+");
                    Double b_lat = Double.parseDouble(b_separator[1]);
                    System.out.println("index 1 fine, b_lat: " + Double.toString(b_lat));
                    Double b_lon = Double.parseDouble(b_separator[2]);
                    System.out.println("index 2 fine, b_lon: " + Double.toString(b_lon));

                    return Double.toString(Math.sqrt(Math.pow(b_lat - a_lat, 2) + Math.pow(b_lon - a_lon, 2)));
                }
                )
                .toStream((Windowed<String> k, String v) -> k.key())
                .filter((String k, String v) -> Double.parseDouble(v) <= d_thresh)
                .to("geo-events");


        // Detect when objects enter or leave a geofence with radius "rad" (in meters) and center "center"
        Double rad = 10.0;
        Double[] center = {0.0, 0.0};
        mainDataStream.filter((key, value) -> key.equalsIgnoreCase("geo"))
                .groupBy((k, v) -> "")
                .windowedBy(TimeWindows.of(Duration.ofSeconds(window_size)).advanceBy(Duration.ofSeconds(advance_by)))
                .aggregate(() -> Double.toString(Double.POSITIVE_INFINITY),
                        (k, v, acc) -> {
                            String[] separator = v.split("\\s+");
                            Double lat = Double.parseDouble(separator[1]);
                            Double lon = Double.parseDouble(separator[2]);
                            return Double.toString(Math.min(Math.sqrt(Math.pow(lat - center[0], 2) +
                                    Math.pow(lon - center[1], 2)), Double.parseDouble(acc)));
                        }

                )
                .toStream((Windowed<String> k, String v) -> k.key())
                .filter((String k, String v) -> Double.parseDouble(v) <= rad)
                .to("geo-events");


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

class AverageContainer {
    public double count;
    public double sum;
    public AverageContainer(double count, double sum) {
        this.count = count;
        this.sum = sum;
    }
}
