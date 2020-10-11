package kafkaRouting;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

/*
 * =====================================================================================================================
 *  This file defines a microservice that is responsible for sorting the main data stream into event specific substreams.
 *  These substreams will contain only the events of the type specified by the topic.
 *          EX) The topic 'type-A' is a substream that will hold event records of event type A ONLY.
 * =====================================================================================================================
 */

public class MainDataRouting {
    public static void main(String[] args) throws InterruptedException {
        // Define properties map that specifies kafka streams configuration parameters
        Properties streamProps = new Properties();
        streamProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "sort-main-stream");
        streamProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        streamProps.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamProps.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamProps.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 1);

        // Define processor node topology and stream topic routing
        final StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> mainDataStream = builder.stream("all-event-data");

        KStream<String, String>[] branches = mainDataStream.branch(
                (key, value) -> key.equalsIgnoreCase("0"),
                (key, value) -> key.equalsIgnoreCase("1"),
                (key, value) -> key.equalsIgnoreCase("2"),
                (key, value) -> key.equalsIgnoreCase("3"),
                (key, value) -> key.equalsIgnoreCase("4"),
                (key, value) -> key.equalsIgnoreCase("5"),
                (key, value) -> key.equalsIgnoreCase("100")
                );
        
        branches[0].to("type-A");
        branches[1].to("type-B");
        branches[2].to("type-C");
        branches[3].to("type-D");
        branches[4].to("type-E");
        branches[5].to("type-F");
        branches[6].to("type-pos");

        // Inspect the generated topology
        final Topology ingressTopology = builder.build();
        System.out.println(ingressTopology.describe());

        // Construct the streams using the defined topology and properties
        final KafkaStreams streams = new KafkaStreams(ingressTopology, streamProps);


        // Close the streams client when user throws an interrupt with ctrl-c
        final CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread("MainDataRouting-shutdown-hook") {
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
    }
}
