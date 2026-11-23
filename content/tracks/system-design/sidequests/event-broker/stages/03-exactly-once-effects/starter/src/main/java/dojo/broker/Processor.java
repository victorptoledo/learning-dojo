package dojo.broker;

/** A consumer that applies the effects of each message exactly once. See README.md. */
public final class Processor {

    public Processor(Broker broker, String group, String topic, String member, Store store, Handler handler) {
    }

    public int runOnce(int max) {
        throw new UnsupportedOperationException("todo: runOnce");
    }
}
