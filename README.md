# BISON
(OdinsWrath/Jared Keefer's Kafka Branch)


## Documentation
This illustrates the intricasies of the Kafka sorting demo found in the KafKaEventSorter directory.


### Basic Kafka Console Operations:

This section covers the provided console interface for Apache Kafka. In these examples, I have named the installation folder for my kafka as "Kafka" this directory may change for you. Also, if running this on linux/mac change the intitial directory specification from bin\windows to just bin/<program_name>.sh

#### START ZOOKEEPER @ localhost:2181  
<pre><code>...\Kafka>bin\windows\zookeeper-server-start.bat config\zookeeper.properties </code></pre>

#### START KAFKA SERVER @ localhost:9092  
<pre><code>...\Kafka>bin\windows\kafka-server-start.bat config\server.properties </code></pre>

#### CREATE A TOPIC CALLED 'topic_name'  
<pre><code>...\Kafka\bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topic_name </code></pre>

#### DISPLAY EXISTING TOPICS  
<pre><code>...\Kafka>bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092 </code></pre>

#### GENERATE CONSOLE CONSUMER FOR GIVEN 'topic_name' & DISPLAY ALL PREVIOUS CONTENTS  
<pre><code>...\Kafka>bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic topic_name --from-beginning --formatter kafka.tools.DefaultMessageFormatter --property print.key=true --property print.value=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer </code></pre>

#### GENERATE CONSOLE PRODUCER FOR GIVEN 'topic_name'  
<pre><code>...\Kafka>bin\windows\kafka-console-producer.bat --topic topic_name --broker-list localhost:9092 --property "parse.key=true" --property "key.separator=:" </code></pre>
`NOTE:` This opens up a connection where you can input a key value pair to the specified
topic with the syntax 'key:value'


### How To Run The Demo:
This section details how to run the demo so that the funcitonality is clearly displayed.
`NOTE:` This demonstration was created using the IntelliJ IDEA. If you wish to compile/run this demo without using and IDE, you must run/compile with proper specifications for a Maven project.   

- Start Zookeeper and Kafka server  
- Run CreateTopics.bat (if running linux/mac you can simply update this file to adhere to the shell you are using)  
- Run ProduceData.java  
- Run MainDataRouting.java  
- Generate a console consumer for the topic you wish to view using the command provided above  
- Rerun ProduceData.java with the console consumer active to see the routing  take place


### File and Demo Program Explanation:
#### CreateTopics.bat
This is a simple batch file that uses the topic creation command listed above. The topics created are; type-A, type-B, type-C, type-D, type-E, and type-pos. These topics directly correlate to the specified event types outlined in the specified use cases. For example, the demo will route all type A events to the Kafka topic 'type-A'.  
`NOTE:` For this file to be effective, you must have Zookeeper and a Kafka server currently running. This file need only be run one time; the topics persist across restarts of the Kafka server and zookeeper server. If you delete the log files from zookeeper and kafka server this persistence is lost.  


#### ProduceData.java
This file creates a new kafka producer that then sends data over the main data stream (ie. into the all-event-data topic). This producer sends 1 of each event type over the stream. This is accomplished by first storing a set of properties that define the configuration of the desired producer within a java.util.Properties object. Then a Kafka producer object (`KafkaProducer\<Key_Type, Value_Type\>`) is created and configured to use these properties. The definition of this KafkaProducer object also defines the serialization method for each event passed to the stream from this KafkaProducer. The final poriton of this file simply creates producer records that represent each event type and sends them over the data stream.  
`NOTE:` For this file to function properly, it must be run alongside an active instance of zookeeper, Kafka server, and MainDataRouting.


#### MainDataRouting.java
This file defines how Kafka sorts/reroutes each ingressed event to its proper topic. This is accomplished by first specifying the configuration of the data stream through a Properties object. Then a `KStream\<Key_Type, Value_Type\>` object is configured to use these properties. This KStream represents the main data stream from external devices into CEP solution. This data stream is then split into seven separate sub streams based on the value of the key parameter in each event payload. These substreams are stored into a KStream array and then routed to the desired topic using the KStream.to() command. This command routes stream data from the current stream, in this case a subset of the main data stream, to the specified topic. Therefore, this command also defines the topology of the system. The final portion of this file ensures that the routing service stops when the streams are closed.
