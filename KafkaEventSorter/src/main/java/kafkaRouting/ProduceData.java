package kafkaRouting;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Date;
import java.util.Properties;

/*
 * =====================================================================================================================
 *  This file creates a new kafka producer that then sends data over the main data stream (ie. into the all-event-data
 *  topic). This producer sends 1 of each event type over the stream.
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
        String[] eventData = {"N/A", "N/A", "N/A", "N/A", "170", "-22", "Lat: 40.0150 N   Long: 105.2705 W   Acc: 1"};

        for(int i=0; i<7; i++) {
            String payload = eventData[i] + "      TS:" + new Date();
            if (i == 6) i = 100;        /* Handles location data key value being 100 */
            //System.out.println(i + " Message from java code " + new Date());
            ProducerRecord<String, String> record = new ProducerRecord<String, String>("all-event-data", Integer.toString(i), payload);
            kProd.send(record);
            Thread.sleep(1000); // Used to illustrate effective timestamping
        }

        kProd.close();
    }
}