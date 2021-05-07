# Kafka/Trill integration

## Requirements

It is *highly* recommended to run Kafka on either a Linux machine or on Windows Subsystem for Linux 2 (WSL 2). *Note: WSL2 should not be used for production environments.*

Download the [Confluent Platform](https://docs.confluent.io/confluent-cli/current/install.html) binary as well an instance of [Apache Kafka](https://kafka.apache.org/downloads). Add the `confluent` binary and the Kafka binaries to your `PATH`. You should be able to run

    confluent local services start

which will start all services associated with Confluent. Please bear in mind steps may differ in a cloud deployment setting.

Next, ensure the latest version of Visual Studio is installed and launch the `TrillXPF.sln` file found in the `TrillXPF` directory. **In case of WSL2:** ensure that the "WSL2" run profile is selected in Visual Studio.

### Running on Linux/WSL2

Then, run the Kafka Avro producer from Java. This may require the Maven package:

    mvn exec:java -Dexec.mainClass=kafkaRouting.AvroProducer

You should be able to see the Kafka topic contents using:

    sudo kafka-avro-console-consumer --bootstrap-server localhost:9092 --topic all-event-data-test --from-beginning

Furthermore, in the "Debug" window in Visual Studio, Microsoft Trill should be displaying event data.
