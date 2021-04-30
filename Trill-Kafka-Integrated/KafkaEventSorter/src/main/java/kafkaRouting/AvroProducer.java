package kafkaRouting;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.Properties;
import java.time.Instant;
import java.util.Random;

public class AvroProducer {
    public static void main(String[] args) {
        Random rand = new Random();
        // all taken/derived from confluent avro schema tutorial

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                  org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                  io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        props.put("schema.registry.url", "http://localhost:8081");
        KafkaProducer producer = new KafkaProducer(props);

        String key = "geo";

        // nested schema to define fields for the payload data
        // NOTE: record name fields can be changed, collision with lower
        // record variable names is not required or desired
        String eventDataSchemaDefinition =
            "{" +
            "`namespace`:`TrillXPF`," +
            "`name`:`EventDataRecord`," +
            "`type`:`record`," +
            "`fields`: [" +
                "{`name`:`Latitude`,`type`:`float`}," +
                "{`name`:`Longitude`,`type`:`float`}," +
                "{`name`:`Accuracy`,`type`:`int`}" +
                "]" +
            "}";
        // generic schema for any event, extensible through eventDataRecord def
        String eventSchemaDefinition =
            "{" +
            "`namespace`:`TrillXPF`," +
            "`name`:`EventRecord`," +
            "`type`:`record`," +
            "`fields`: [" +
                "{`name`:`DeviceID`,`type`:`int`}," +
                "{`name`:`Timestamp`,`type`:`long`}," +
                "{`name`:`EventID`,`type`:`int`}," +
                "{`name`:`EventData`,`type`:`EventDataRecord`}" +
                "]" +
            "}";
        eventSchemaDefinition = eventSchemaDefinition.replace('`','"');
        eventDataSchemaDefinition = eventDataSchemaDefinition.replace('`','"');

        // parse schema definitions
        Schema.Parser parser = new Schema.Parser();
        Schema eventDataSchema = parser.parse(eventDataSchemaDefinition);
        Schema eventSchema = parser.parse(eventSchemaDefinition);
        // ^doing this second lets the parser remember the eventDataSchemaDefinition
        // so we can reference it in eventSchemaDefinition
        GenericRecord eventRecord = new GenericData.Record(eventSchema);
        GenericRecord eventDataRecord = new GenericData.Record(eventDataSchema);

        // create event record field by field
        eventRecord.put("DeviceID", rand.nextInt(99999));
        eventRecord.put("Timestamp", Instant.now().getEpochSecond());
        eventRecord.put("EventID", rand.nextInt(99999));

        // created nested event data record
        eventDataRecord.put("Latitude", 50.24);
        eventDataRecord.put("Longitude", 55.30);
        eventDataRecord.put("Accuracy", 200);
        eventRecord.put("EventData", eventDataRecord);

        // hit send
        ProducerRecord<Object, Object> record = new ProducerRecord<>("all-event-data-test", key, eventRecord);
        producer.send(record);

        // When you're finished producing records, you can flush the producer to ensure it has all been written to Kafka and
        // then close the producer to free its resources.
        producer.flush();
        producer.close();
    }
}
