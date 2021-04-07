# BISON - Kafka Stand-Alone Solution


## Description
This repo holds the Kafka stand-alone solution to the provided use cases. For this project to function, you must have a working installation of Apache Kafka. Kafka can be downloaded from the following link: https://kafka.apache.org/downloads


### Basic Kafka Console Operations:

This section covers the provided console interface for Apache Kafka. In these examples, I have named the installation folder for my kafka as "Kafka" this directory may change for you. Also, if running this on linux/mac change the intitial directory specification of all commands from bin\windows to bin/<program_name>.sh

#### START ZOOKEEPER @ localhost:2181  
<pre><code>...\Kafka>bin\windows\zookeeper-server-start.bat config\zookeeper.properties </code></pre>

#### START KAFKA SERVER @ localhost:9092  
<pre><code>...\Kafka>bin\windows\kafka-server-start.bat config\server.properties </code></pre>

#### CREATE A TOPIC CALLED 'topic_name'  
<pre><code>...\Kafka\bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topic_name </code></pre>

#### DISPLAY EXISTING TOPICS  
<pre><code>...\Kafka>bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092 </code></pre>

#### GENERATE CONSOLE CONSUMER THAT SUBSCRIBES TO 'topic_name' & DISPLAY ALL PREVIOUS CONTENTS  
<pre><code>...\Kafka>bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic topic_name --from-beginning --formatter kafka.tools.DefaultMessageFormatter --property print.key=true --property print.value=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer </code></pre>

#### GENERATE CONSOLE PRODUCER THAT WILL WRITE TO 'topic_name'  
<pre><code>...\Kafka>bin\windows\kafka-console-producer.bat --topic topic_name --broker-list localhost:9092 --property "parse.key=true" --property "key.separator=:" </code></pre>
`NOTE:` This opens up a connection where you can input a key value pair to the specified
topic with the syntax 'key:value'


## How To Run The Demo:
This section details how to run the demo so that the funcitonality is clearly displayed.
`NOTE:` This demonstration was created using the IntelliJ IDEA. If you wish to compile/run this demo without using and IDE, you must run/compile with proper specifications for a Maven project.   

### Initial Setup
- Start Zookeeper and Kafka server  
- Run CreateTopics.bat (Windows) or CreateTopics.sh (Linux)  
- Start MainDataRouting.java  
- Generate a console producer that will write to the 'all-event-data' topic/stream.   

#### Demo: Temporal
There are two temporal use cases that are triggered by this prototype. If either of these occurs, a record of the event is passed to the 'temporal-events' topic. To view this behavior first generate a console consumer that subscribes to the 'temporal-events' topic; then follow the instructions detailed below.  
1. Determine if event type A happens 3+ times within 1 second.
    * From the console producer, pass in 3 events of the form "<any_key_value>:A"
    * If these events were passed in within the allotted time, you will see a new record of this occurrence appear in your console consumer.
2. Determine if event type B happens within 5 seconds of event type A.
    * From the conole producer:
        * Pass in an event of the form "<any_key_value>:A"
        * Pass in an event of the form "<any_key_value>:B"
    * If the two events were passed within the allotted threshold of 5 seconds, you will see a new record of this occurrence will appear in your console consumer.
     

#### Demo: Evaluation
This processing determines if the value of an event is strictly greater than the given threshold of 10.
- Generate a console consumer that subscribes to the 'evaluation-events' topic.  
- From the console producer, pass in an event of the form "<any_key_value>:<any_number>"
- If the <any_number> field is strictly greater than 10, you will see a new record of this occurrence appear in your console consumer.

#### Demo: Sequence
This processing determines if events of type {A, B, C} and {A, B, C, D} occur in the respective sequence.
- Generate a console consumer that subscribes to the 'sequence-events' topic.  
- From the console producer, pass in events of the form "<any_key_value>:<any_type>
    * NOTE: the parameter <any_type> can be any string, but to trigger the occurrence it must follow the provided sequence of characters.
- If the events that were passed in follow the sequence {A, B, C} and/or {A, B, C, D}, a new record of this occurrence will appear in your console consumer.
    * NOTE: If the sequence {A, B, C, D} is passed in, you will see two resultant records in the console consumer. One for the intial trigger of encountering {A, B, C}. The second representing the found sequence of {A, B, C, D}

#### Demo: Geospatial


### File and Implementation Explanation:
#### CreateTopics.bat
This is a simple batch file that uses the topic creation command listed above. The topics created are; type-A, type-B, type-C, type-D, type-E, and type-pos. These topics directly correlate to the specified event types outlined in the specified use cases. For example, the demo will route all type A events to the Kafka topic 'type-A'.  
`NOTE:` For this file to be effective, you must have Zookeeper and a Kafka server currently running. This file need only be run one time; the topics persist across restarts of the Kafka server and zookeeper server. If you delete the log files from zookeeper and kafka server this persistence is lost.  


#### ProduceData.java
This file creates a new kafka producer that then sends data over the main data stream (ie. into the all-event-data topic). This producer sends 1 of each event type over the stream. This is accomplished by first storing a set of properties that define the configuration of the desired producer within a java.util.Properties object. Then a Kafka producer object (`KafkaProducer\<Key_Type, Value_Type\>`) is created and configured to use these properties. The definition of this KafkaProducer object also defines the serialization method for each event passed to the stream from this KafkaProducer. The final poriton of this file simply creates producer records that represent each event type and sends them over the data stream.  
`NOTE:` For this file to function properly, it must be run alongside an active instance of zookeeper, Kafka server, and MainDataRouting.


#### MainDataRouting.java
This file defines how Kafka sorts/reroutes each ingressed event to its proper topic. This is accomplished by first specifying the configuration of the data stream through a Properties object. Then a `KStream\<Key_Type, Value_Type\>` object is configured to use these properties. This KStream represents the main data stream from external devices into CEP solution. This data stream is then split into seven separate sub streams based on the value of the key parameter in each event payload. These substreams are stored into a KStream array and then routed to the desired topic using the KStream.to() command. This command routes stream data from the current stream, in this case a subset of the main data stream, to the specified topic. Therefore, this command also defines the topology of the system. The final portion of this file ensures that the routing service stops when the streams are closed.
