
These instructions are after installing the `kafka` package on Arch Linux.

Start the Kafka service:

    systemctl start kafka.service

This should start the `zookeeper@kafka.service`. If this doesn't work, delete the logs from the directory specified in `/usr/share/kafka/server.properties`, which by default is `/var/log/kafka`.

From the KafkaEventSorter directory, use `mvn` to build the Maven project.

    mvn exec:java -Dexec.mainClass=kafkaRouting.MainDataRouting

    mvn exec:java -Dexec.mainClass=kafkaRouting.AgentInterface
