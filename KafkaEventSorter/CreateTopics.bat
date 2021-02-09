
:: =====================================================================================================================
::  This file creates the necessary kafka topics to interface with MainDataRouting.java. These topics are arranged
::  such that each event type has its own kafka topic.
::          NOTE: For this file to be effective, you must have zookeeper and kafka server currently running.
::                This file need only be run one time; the topics persist across restarts of the kafka server
::                and zookeeper server. If you delete the log files from zookeeper and kafka server this persistence
::                is lost.
::=====================================================================================================================

:: PUT IN THE FULL DIRECTORY PATH TO YOUR KAFKA INSTALLATION IN THE FOLLOWING LINE AFTER '/D'
@ echo off
 cd /D C:\Kafka
::
call bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1^
 --partitions 1 --topic all-event-data
::
call bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1^
 --partitions 1 --topic temporal-events
::
call bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1^
 --partitions 1 --topic type-B
::
call bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1^
 --partitions 1 --topic type-C
::
call bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1^
 --partitions 1 --topic type-D
::
call bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1^
 --partitions 1 --topic type-E
::
call bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1^
 --partitions 1 --topic type-F
::
call bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1^
 --partitions 1 --topic type-pos
