#!/usr/bin/sh
# =====================================================================================================================
#  This file creates the necessary kafka topics to interface with MainDataRouting.java. These topics are arranged
#  such that each event type has its own kafka topic.
#          NOTE: For this file to be effective, you must have zookeeper and kafka server currently running.
#                This file need only be run one time; the topics persist across restarts of the kafka server
#                and zookeeper server. If you delete the log files from zookeeper and kafka server this persistence
#                is lost.
#=====================================================================================================================

# path to kafka installation
kafka_dir=/usr/share/kafka

$kafka_dir/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic all-event-data

$kafka_dir/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic temporal-event

$kafka_dir/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic type-B

$kafka_dir/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic type-C

$kafka_dir/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic type-D

$kafka_dir/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic type-E

$kafka_dir/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic type-F

$kafka_dir/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic type-pos
