package kafkaRouting;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class TemporalProcessor {
    private TopicConsumer consumer;
    private ConsumerRecords<String,String> masterRecord;
    private int testMillis = 8000;

    TemporalProcessor() {
        TopicConsumer consumer = new TopicConsumer("temporal-event");
        masterRecord = consumer.getConsumerRecords();
    }
    public void watchRecordsAndProcess() {
        System.out.println("updating record");
        Thread t1 = new Thread( () -> timeSensitive("A","B", this.testMillis) );
        Thread t2 = new Thread( () -> multipleOccurences("A", this.testMillis, 3) );

        t1.start();
        t2.start();

        while (true) {
            System.out.println("updating record");
            masterRecord = consumer.getConsumerRecords();
        }
    }
    Timestamp parseRecord(String event) {
        String[] splitData = event.split("\\s+");
        return new Timestamp(splitData[0], Integer.parseInt(splitData[1]));
    }

    void timeSensitive(String eventA, String eventB, long millis) {
        boolean foundA = false;
        long aTime = 0;
        Timestamp ts;
        ConsumerRecords<String,String> records;
        while (true) {
            records = this.masterRecord;
            // foreach record
            for (ConsumerRecord<String,String> record : records) {
                // System.out.println(record.value());
                ts = parseRecord(record.value());
                if (!foundA && ts.eventType == eventA) {
                    foundA = true;
                    aTime = ts.time;
                }
                if (aTime - ts.time > millis) {
                    foundA = false;
                }
                if (foundA && ts.eventType == eventB) {
                    foundA = false;
                    System.out.println(eventA + " happened at timestamp " + Long.toString(aTime) + " and " + eventB + " happened at timestamp " + Long.toString(ts.time) + " (within " + Long.toString(millis) + " ms)");
                }
            }
            // records.forEach(record -> {
            //     Timestamp ts = parseRecord(record.value());
            //     if (!foundA && ts.eventType == eventA) {
            //         foundA = true;
            //         aTime = ts.time;
            //     }
            //     if (aTime - ts.time > millis) {
            //         foundA = false;
            //     }
            //     if (foundA && ts.eventType == eventB) {
            //         foundA = false;
            //         System.out.println(eventA + " happened at timestamp " + Long.toString(aTime) + " and " + eventB + " happened at timestamp " + Long.toString(ts.time) + " (within " + Long.toString(millis) + " ms)");
            //     }
            // });
        }
    }
    void multipleOccurences(String eventA, long millis, int n) {
        int timesLeft = n;
        long aTime = 0;
        Timestamp ts;
        boolean seenOnce = false;
        ConsumerRecords<String,String> records;
        while (true) {
            records = this.masterRecord;
            for (ConsumerRecord<String,String> record : records) {
                ts = parseRecord(record.value());

                if (!seenOnce && ts.eventType == eventA) {
                    seenOnce = true;
                    timesLeft -= 1;
                    aTime = ts.time;
                }
                if (aTime - ts.time > millis) {
                    seenOnce = false;
                    timesLeft = n;
                }
                if (seenOnce && ts.eventType == eventA) {
                    timesLeft -= 1;
                    if (timesLeft <= 0) {
                        timesLeft = n;
                        seenOnce = false;
                        System.out.println("Event " + eventA + " happened for the " + Integer.toString(n) + "th time within " + Long.toString(millis) + " ms!");
                    }
                }
            }
            // foreach record
            // records.forEach(record -> {
            //     Timestamp ts = parseRecord(record.value());
            //     if (!seenOnce && ts.eventType == eventA) {
            //         seenOnce = true;
            //         timesLeft -= 1;
            //         aTime = ts.time;
            //     }
            //     if (aTime - ts.time > millis) {
            //         seenOnce = false;
            //         timesLeft = n;
            //     }
            //     if (seenOnce && ts.eventType == eventA) {
            //         timesLeft -= 1;
            //         if (timesLeft <= 0) {
            //             timesLeft = n;
            //             seenOnce = false;
            //             System.out.println("Event " + eventA + " happened for the " + Integer.toString(n) + "th time within " + Long.toString(millis) + " ms!");
            //         }
            //     }
            // });
        }
    }
}

class Timestamp {
    public String eventType;
    public long time;
    Timestamp(String eventType, long time) {
        this.eventType = eventType;
        this.time = time;
    }
}
