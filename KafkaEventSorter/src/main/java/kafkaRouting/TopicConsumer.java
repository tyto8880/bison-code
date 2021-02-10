package kafkaRouting;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

/*
 * =====================================================================================================================
 *  This class defines the basic connection parameters, and functionality, of a generalized Kafka consumer.
 *
 *          NOTES:
 *                - For an object of this class to be created, it must be created with active instances of; zookeeper,
 *                  the kafka server with all subtopics created (via CreateTopics.bat), and MainDataRouting.
 *                - Objects of this class must be created by passing a String parameter that defines the desired
 *                  topic that the consumer will subscribe to.
 * =====================================================================================================================
 */

public class TopicConsumer {
    private Properties connectionProp;
    private final static String BOOTSTRAP_SERVERS = "localhost:9092";  // Can be updated whenever brokers are added/changed
    private KafkaConsumer<String, String> kCons;

    // Constructor: Set connection parameters and initialize a Kafka consumer that subscribes to the specified topic
    TopicConsumer(String topic) {
        // Set connection parameters
        // connectionProp.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        connectionProp.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        connectionProp.put(ConsumerConfig.GROUP_ID_CONFIG, "TopicConsumer");
        connectionProp.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        connectionProp.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Create the consumer and make it subscribe to the topic 'topic'
        kCons = new KafkaConsumer<String, String>(connectionProp);
        kCons.subscribe(Collections.singletonList(topic));
    }

    public ConsumerRecords<String,String> getConsumerRecords() {
        final ConsumerRecords<String,String> records = kCons.poll(1000);
        kCons.commitAsync();
        return records;
    }

    
}
