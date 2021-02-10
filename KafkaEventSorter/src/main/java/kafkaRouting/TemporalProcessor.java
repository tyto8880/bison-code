package kafkaRouting;

public class TemporalProcessor {
    private TopicConsumer consumer;
    private ConsumerRecords<String,String> masterRecord;
    private int testseconds = 1;

    TemporalProcessor() {
        TopicConsumer consumer = new TopicConsumer("temporal-event");
        masterRecord = consumer.getConsumerRecords();
    }
    watchRecordsAndProcess() {
        Thread t1 = new Thread( () -> multipleOccurences("A","B", this.testseconds) );
        Thread t2 = new Thread( () -> timeSensitive("A", "B", this.testseconds, 5) );

        t1.start()
        t2.start()

        while (true) {
            masterRecord = consumer.getConsumerRecords();
        }
    }
    Timestamp parseRecord(String event) {
        String[] splitData = event.split("\\s+");
        return new Timestamp(splitData[0], Integer.parseInt(splitData[1])
    }

    timeSensitive(String eventA, String eventB, int seconds) {
        boolean foundA = false;
        int aTime = 0;
        while (true) {
            record = this.masterRecord;
            // foreach record
            Timestamp ts = parseRecord(record.value);
            if (!foundA && ts.eventType == "A") {
                foundA = true;
                aTime = ts.time;
                continue;
            }
            if (aTime - ts.time > seconds) {
                foundA = false;
            }
            if (foundA && ts.eventType == "B") {
                foundA = false;
                System.out.println(eventA + " happened at timestamp " + Integer.toString(aTime) + " and " + eventB + " happened at timestamp " + Integer.toString(ts.time) + " (within " + seconds + " seconds)");
            }
        }
    }
    multipleOccurences(String eventA, String eventB, int seconds, int n) {
        while (true) {
            record = this.masterRecord;
        }
    }
}

public class Timestamp {
    public String eventType;
    public int time;
    Timestamp(string eventType, int time) {
        this.eventType = eventType;
        this.time = time;
    }
}
