# Kafka/Trill integration

To run on linux:

Install Confluent Platform. Supposed to be able to run:

    confluent local services start

which will start all services associated with confluent, even the ones we don't need. Regardless, that didn't work for me. Instead, I start the necessary services individually:

    sudo systemctl start confluent-zookeeper.service
    sudo systemctl start confluent-server.service # this is the kafka broker
    sudo systemctl start confluent-schema-registry.service

-- might need to install Confluent CLI: https://docs.confluent.io/confluent-cli/current/install.html; this has the "confluent local" commands.

Then, run the Kafka Avro producer from Java:

    mvn exec:java -Dexec.mainClass=kafkaRouting.AvroProducer

You should be able to see the output using:

    sudo kafka-avro-console-consumer --bootstrap-server localhost:9092 --topic all-event-data-test --from-beginning
