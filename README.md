# BISON
(OdinsWrath/Jared Keefer's Kafka Branch)

## Documentation
This illustrates the intricasies of the Kafka sorting demo found in the KafKaEventSorter directory.

### Basic Kafka Console Operations

This section covers the provided console interface for Apache Kafka. In these examples, I have named the installation folder for my kafka as "Kafka" this directory may change for you. Also, if running this on linux/mac change the intitial directory specification from bin\windows to just bin/<program_name>.sh

START ZOOKEEPER @ localhost:2181  
<pre><code>...\Kafka>bin\windows\zookeeper-server-start.bat config\zookeeper.properties </code></pre>

START KAFKA SERVER @ localhost:9092  
<pre><code>...\Kafka>bin\windows\kafka-server-start.bat config\server.properties </code></pre>

CREATE A TOPIC CALLED <topic_name>  
<pre><code>...\Kafka\bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic <topic_name> </code></pre>

CHECK CURRENT TOPICS  
<pre><code>...\Kafka>bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092 </code></pre>

GENERATE CONSOLE CONSUMER FOR GIVEN <topic_name> & DISPLAY ALL PREVIOUS CONTENTS  
<pre><code>...\Kafka>bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic <topic_name> --from-beginning --formatter kafka.tools.DefaultMessageFormatter --property print.key=true --property print.value=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer </code></pre>

GENERATE CONSOLE PRODUCER FOR GIVEN <topic_name>  
<pre><code>...\Kafka>bin\windows\kafka-console-producer.bat --topic all-event-data --broker-list localhost:9092 --property "parse.key=true" --property "key.separator=:" </code></pre>
NOTE: This opens up a connection where you can input a key value pair to the specified
topic with the syntax key:value
