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
        Thread t1 = new Thread( () -> timeSensitive("A","B", this.testseconds) );
        Thread t2 = new Thread( () -> multipleOccurences("A", this.testseconds, 3) );

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
            records = this.masterRecord;
            // foreach record
            records.forEach(record -> {
                Timestamp ts = parseRecord(record.value);
                if (!foundA && ts.eventType == eventA) {
                    foundA = true;
                    aTime = ts.time;
                }
                if (aTime - ts.time > seconds) {
                    foundA = false;
                }
                if (foundA && ts.eventType == eventB) {
                    foundA = false;
                    System.out.println(eventA + " happened at timestamp " + Integer.toString(aTime) + " and " + eventB + " happened at timestamp " + Integer.toString(ts.time) + " (within " + seconds + " seconds)");
                }
            }
        }
    }
    multipleOccurences(String eventA, int seconds, int n) {
        int timesLeft = n;
        boolean seenOnce = false;
        while (true) {
            records = this.masterRecord;
            // foreach record
            records.forEach(record -> {
                Timestamp ts = parseRecord(record.value);
                if (!seenOnce && ts.eventType == eventA) {
                    seenOnce = true;
                    timesLeft -= 1;
                    aTime = ts.time;
                }
                if (aTime - ts.time > seconds) {
                    seenOnce = false;
                    timesLeft = n;
                }
                if (seenOnce && ts.eventType == eventA) {
                    timesLeft -= 1;
                    if (timesLeft <= 0) {
                        timesLeft = n;
                        seenOnce = false;
                        System.out.println("Event " + eventA + " happened for the " + Integer.parseInt(n) + "th time within " + Integer.parseInt(seconds) + " seconds!");
                    }

                }
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
