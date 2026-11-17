package dojo.tickets;

import java.time.Instant;
import java.util.List;

/** A hold as the API shows it. */
public record HoldView(long id, long eventId, List<String> seats, long totalCents, Instant expiresAt, HoldStatus status) {
    static HoldView of(Hold hold, HoldStatus status) {
        return new HoldView(hold.getId(), hold.getEventId(), List.copyOf(hold.getSeats()), hold.getTotalCents(),
                hold.getExpiresAt(), status);
    }
}
