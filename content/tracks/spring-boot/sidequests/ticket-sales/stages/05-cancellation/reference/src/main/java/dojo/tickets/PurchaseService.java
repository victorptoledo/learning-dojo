package dojo.tickets;

import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Turns holds into orders, once per idempotency key. */
@Service
public class PurchaseService {
    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_-]{8,64}");

    /** The order, and whether this request created it (201) or found it (200). */
    public record Purchase(TicketOrder order, boolean created) {
    }

    private final EventService events;
    private final HoldRepository holds;
    private final HoldService holdService;
    private final SeatRepository seats;
    private final OrderRepository orders;

    public PurchaseService(EventService events, HoldRepository holds, HoldService holdService, SeatRepository seats,
            OrderRepository orders) {
        this.events = events;
        this.holds = holds;
        this.holdService = holdService;
        this.seats = seats;
        this.orders = orders;
    }

    @Transactional
    public Purchase purchase(long holdId, String key, String buyer) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new FieldException("Idempotency-Key", "8 to 64 letters, digits, - or _");
        }
        long eventId = holds.eventOf(holdId, buyer)
                .orElseThrow(() -> new NotFoundException("hold " + holdId + " not found"));
        // The event row first, then the hold: a cancellation cannot slip in between, and while
        // these locks are held every other purchase of the hold waits here.
        Event event = events.lock(eventId);
        Hold hold = holds.lock(holdId).orElseThrow();
        // Only after the lock: a retry that waited now sees the order the first one made.
        Optional<TicketOrder> earlier = orders.findByBuyerAndIdempotencyKey(buyer, key);
        if (earlier.isPresent()) {
            if (earlier.get().getHoldId() != holdId) {
                throw new RuleException("idempotency key already used for another purchase");
            }
            return new Purchase(earlier.get(), false);
        }
        EventService.ensureOnSale(event);
        switch (holdService.statusOf(hold)) {
            case EXPIRED -> throw new GoneException("hold " + holdId + " has expired");
            case PURCHASED -> throw new ConflictException("hold " + holdId + " is already purchased");
            case RELEASED -> throw new ConflictException("hold " + holdId + " is not active");
            case HELD -> {
            }
        }
        seats.lockHeldBy(holdId).forEach(Seat::sell);
        hold.setStatus(HoldStatus.PURCHASED);
        return new Purchase(orders.saveAndFlush(new TicketOrder(hold, key)), true);
    }

    @Transactional(readOnly = true)
    public TicketOrder get(long id, String buyer) {
        return orders.findById(id).filter(o -> o.getBuyer().equals(buyer))
                .orElseThrow(() -> new NotFoundException("order " + id + " not found"));
    }
}
