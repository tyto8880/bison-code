package kafkaRouting;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/*
 * =====================================================================================================================
 *  This file creates a new kafka producer that then sends data over the main data stream (ie. into the all-event-data
 *  topic). This producer sends 1 of each event type over the stream. Used ONLY for testing system routing capabilities.
 *          NOTE: For this file to function properly, it must be run alongside an active instance of zookeeper,
 *                the kafka server, and MainDataRouting.
 * =====================================================================================================================
 */


public class ProduceData {
    public static void main(String[] args) throws InterruptedException {
        Properties prodProp = new Properties();

        prodProp.setProperty("bootstrap.servers", "localhost:9092");
        prodProp.put("acks", "all");
        //prodProp.setProperty("kafka.topic.name", "TestTopic");

        KafkaProducer<String, String> kProd = new KafkaProducer<String, String>(prodProp, new StringSerializer(), new StringSerializer());

        // Define different dummy payloads representing the different event types
        // String[] eventData = {"N/A", "N/A", "N/A", "N/A", "170", "-22", "Lat: 40.0150 N   Long: 105.2705 W   Acc: 1"};


        // Sends 2 events with a string payload to the main stream 'all-event-data' with 1 millisecond between them
            // Commented out to test sequence event processing
        for(int i=4; i<6; i++) {
            String payload = "b " + Integer.toString(i) + " " + Integer.toString(i);
            ProducerRecord<String, String> event = new ProducerRecord<String, String>("all-event-data", "geo", payload);
            kProd.send(event);
            // Thread.sleep(1); // Used to illustrate effective timestamping
        }
        for(int i=0; i<3; i++) {
            String payload = "a " + Integer.toString(i) + " " + Integer.toString(i);
            ProducerRecord<String, String> event = new ProducerRecord<String, String>("all-event-data", "geo", payload);
            kProd.send(event);
            // Thread.sleep(1); // Used to illustrate effective timestamping
        }

        kProd.close();
    }
}
