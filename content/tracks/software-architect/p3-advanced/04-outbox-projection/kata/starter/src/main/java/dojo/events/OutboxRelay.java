package dojo.events;

/** Moves events from the outbox to the broker, at least once and in order. */
public class OutboxRelay {
    public OutboxRelay(Database db, Broker broker) {
    }

    /** Publishes the unsent messages and returns how many were published. */
    public int relay() {
        throw new UnsupportedOperationException("todo: relay the outbox");
    }
}
