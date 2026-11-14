package dojo;

import java.time.Clock;
import org.springframework.stereotype.Component;

/** Publishes pending outbox events in order; a scheduler would call publishPending every second. */
@Component
public class OutboxRelay {
    private final OutboxRepository outbox;
    private final MessageBroker broker;
    private final Clock clock;

    public OutboxRelay(OutboxRepository outbox, MessageBroker broker, Clock clock) {
        this.outbox = outbox;
        this.broker = broker;
        this.clock = clock;
    }

    public RelayResult publishPending(int batchSize) {
        throw new UnsupportedOperationException("todo publish up to " + batchSize);
    }
}
