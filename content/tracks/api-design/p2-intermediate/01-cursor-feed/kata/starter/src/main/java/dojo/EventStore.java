package dojo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * The event log, in memory. Written for you. Seeded with 50 events (ids 1 to 50) of three
 * types; their times are shuffled and some are equal, as with events that arrive late.
 */
@Component
public class EventStore {
    static final String[] TYPES = {"order.created", "order.paid", "order.shipped"};
    static final Instant SEED_START = Instant.parse("2026-05-01T08:00:00Z");

    private final List<Event> events = new CopyOnWriteArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public EventStore() {
        for (int i = 1; i <= 50; i++) {
            events.add(new Event(nextId.getAndIncrement(), TYPES[i % 3], "order-" + (1000 + i),
                    SEED_START.plus(((i * 7L) % 50) / 2, ChronoUnit.MINUTES)));
        }
    }

    /** Every event, in insertion order. Ordering and paging are the API's job. */
    public List<Event> all() {
        return List.copyOf(events);
    }

    /** A new event that happens now: it is the newest of all. */
    public Event append(String type, String subject) {
        Event event = new Event(nextId.getAndIncrement(), type, subject, Instant.now());
        events.add(event);
        return event;
    }
}
