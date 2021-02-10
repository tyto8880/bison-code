package kafkaRouting;

public class Processor {
    public static void main(String[] args) {
        TemporalProcessor tProc = new TemporalProcessor();

        tProc.watchRecordsAndProcess();
    }
}
