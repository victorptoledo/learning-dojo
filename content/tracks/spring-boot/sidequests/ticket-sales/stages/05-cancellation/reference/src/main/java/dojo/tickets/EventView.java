package dojo.tickets;

import java.time.Instant;

/** An event as the API shows it. */
public record EventView(long id, String name, Instant startsAt, long priceCents, int seats, long available,
        EventStatus status) {
    static EventView of(Event event, long available) {
        return new EventView(event.getId(), event.getName(), event.getStartsAt(), event.getPriceCents(),
                event.getSeatCount(), event.isCancelled() ? 0 : available, event.getStatus());
    }
}
