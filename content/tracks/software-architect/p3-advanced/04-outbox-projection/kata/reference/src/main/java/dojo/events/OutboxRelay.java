package dojo.events;

/** Moves events from the outbox to the broker, at least once and in order. */
public class OutboxRelay {
    private final Database db;
    private final Broker broker;

    public OutboxRelay(Database db, Broker broker) {
        this.db = db;
        this.broker = broker;
    }

    /** Publishes the unsent messages and returns how many were published. */
    public int relay() {
        int published = 0;
        for (OutboxMessage message : db.outbox()) {
            if (message.sent()) {
                continue;
            }
            try {
                broker.publish(message.event());
            } catch (RuntimeException brokerDown) {
                return published;
            }
            String id = message.event().id();
            db.inTransaction(tx -> {
                tx.outbox().replaceAll(m -> m.event().id().equals(id) ? new OutboxMessage(m.event(), true) : m);
                return null;
            });
            published++;
        }
        return published;
    }
}
