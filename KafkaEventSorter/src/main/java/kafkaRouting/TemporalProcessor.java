package kafkaRouting;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import java.util.LinkedList;

public class TemporalProcessor {
    private TopicConsumer consumer;
    private ConsumerRecords<String,String> masterRecord;
    private int testMillis = 8000;

    TemporalProcessor() {
        this.consumer = new TopicConsumer("temporal-event");
        this.masterRecord = consumer.getConsumerRecords();
    }
    public void watchRecordsAndProcess() {
        // System.out.println("updating record");
        Thread t1 = new Thread( () -> timeSensitive("A","B", this.testMillis) );
        Thread t2 = new Thread( () -> multipleOccurences("A", this.testMillis, 3) );

        t1.start();
        t2.start();

        ConsumerRecords<String,String> records;
        while (true) {
            // System.out.println("updating record");
            records = consumer.getConsumerRecords();
            if (!records.isEmpty()) {
                masterRecord = records;
            }
        }
    }
    Timestamp parseRecord(String event) {
        String[] splitData = event.split("\\s+");
        splitData[1] = splitData[1].split(":")[1];
        // System.out.println(splitData[1]);
        return new Timestamp(splitData[0], Long.parseLong(splitData[1]));
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
                // System.out.println("'" + ts.eventType + "'");
                if ((!foundA) && (ts.eventType.equals(eventA))) {
                    System.out.println("A event found");
                    foundA = true;
                    aTime = ts.time;
                }
                if ((aTime - ts.time) > millis) {
                    System.out.println("Timeout");
                    foundA = false;
                }
                if ((foundA) && ts.eventType.equals(eventB)) {
                    System.out.println("B event found");
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
        Timestamp currentTs = new Timestamp("",0);
        LinkedList<Timestamp> tsQueue = new LinkedList<Timestamp>();
        boolean seenOnce = false;
        ConsumerRecords<String,String> records;
        while (true) {
            records = this.masterRecord;
            for (ConsumerRecord<String,String> record : records) {
                ts = parseRecord(record.value());
                if (ts.eventType.equals(eventA) && (tsQueue.size() == 0 || tsQueue.getLast().time != ts.time)) {
                    // System.out.println("added an " + eventA + " at time " + Long.toString(ts.time));
                    tsQueue.add(ts);
                }
                if (tsQueue.getFirst().time - ts.time > millis) {
                    tsQueue.remove();
                }
                if (tsQueue.size() >= n) {
                    System.out.println("Event " + eventA + " happened for the " + Integer.toString(n) + "th time within " + Long.toString(millis) + " ms!");

                    tsQueue.remove();
                }
                // A,,,,,A,,,AA
                // A,,,,,A,A
            }
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
