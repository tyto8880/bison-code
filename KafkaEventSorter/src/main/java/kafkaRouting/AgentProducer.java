package kafkaRouting;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Date;
import java.util.Properties;

/*
 * =====================================================================================================================
 *  This class defines the connection parameters, and functionality, of producers existing on external agents.
 *
 *          NOTE: For an object of this class to be created, it must be created with active instances of; zookeeper,
 *                the kafka server with all subtopics created (via CreateTopics.bat), and MainDataRouting.
 * =====================================================================================================================
 */

public class AgentProducer {
    private Properties connectionProp;
    private KafkaProducer<String, String> kProd;

    // Constructor: Set connection parameters and instantiate a new Kafka producer
    AgentProducer() {
        // Set connection parameters
        connectionProp = new Properties();
        connectionProp.setProperty("bootstrap.servers", "localhost:9092");
        connectionProp.put("acks", "all");

        // Initialize Kafka producer using the defined connection parameters
        kProd = new KafkaProducer<String, String>(connectionProp, new StringSerializer(), new StringSerializer());
    }

    // This function generates a ProducerRecord for a temporal event and sends the package over the "all-event-data"
    // stream.
    public void sendTemporal(String eventType) {
        System.out.println("Sending event type: " + eventType);
        String payload = eventType.toUpperCase() + "      TS:" + Integer.toString(System.currentTimeMillis());
        ProducerRecord<String, String> record = new ProducerRecord<String,String>("all-event-data", Integer.toString(0), payload);
        kProd.send(record);
    }

    // Destructor: Close the producer before destroying AgentProducer objects
    protected void finalize() {
        kProd.close();
    }
}
