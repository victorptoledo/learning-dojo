package dojo;

import java.time.Clock;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public RelayResult publishPending(int batchSize) {
        int published = 0;
        for (OutboxEvent event : outbox.pending(PageRequest.of(0, batchSize))) {
            try {
                broker.publish(event.getEventId(), event.getType(), event.getPayload());
            } catch (RuntimeException e) {
                event.failed();
                return new RelayResult(published, true);
            }
            event.published(clock.instant());
            published++;
        }
        return new RelayResult(published, false);
    }
}
