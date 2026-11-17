package dojo.tickets;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What only the organizer does with an event: cancel it and read its numbers. */
@Service
public class SaleService {
    public record Cancellation(long refundedOrders, long refundedCents, long releasedHolds) {
    }

    public record Report(EventStatus status, int seats, long available, long held, long sold, long paidOrders,
            long revenueCents, long refundedOrders, long refundedCents) {
    }

    private final EventService events;
    private final SeatRepository seats;
    private final HoldRepository holds;
    private final OrderRepository orders;
    private final Clock clock;

    public SaleService(EventService events, SeatRepository seats, HoldRepository holds, OrderRepository orders,
            Clock clock) {
        this.events = events;
        this.seats = seats;
        this.holds = holds;
        this.orders = orders;
        this.clock = clock;
    }

    /**
     * One transaction behind the event lock: every purchase either committed before it (and is
     * refunded here) or waits for it and then finds the event cancelled.
     */
    @Transactional
    public Cancellation cancel(long eventId) {
        Event event = events.lock(eventId);
        if (event.isCancelled()) {
            throw new ConflictException("event " + eventId + " is already cancelled");
        }
        event.cancel();
        OrderTotals paid = orders.totals(eventId, OrderStatus.PAID);
        long activeHolds = holds.countActive(eventId, clock.instant());
        orders.refundAll(eventId);
        holds.releaseAll(eventId);
        seats.releaseAll(eventId);
        return new Cancellation(paid.orders(), paid.cents(), activeHolds);
    }

    @Transactional(readOnly = true)
    public Report report(long eventId) {
        Event event = events.get(eventId);
        Instant now = clock.instant();
        OrderTotals paid = orders.totals(eventId, OrderStatus.PAID);
        OrderTotals refunded = orders.totals(eventId, OrderStatus.REFUNDED);
        long available = event.isCancelled() ? 0 : seats.countFree(eventId, now);
        return new Report(event.getStatus(), event.getSeatCount(), available, seats.countHeld(eventId, now),
                orders.countSoldSeats(eventId), paid.orders(), paid.cents(), refunded.orders(), refunded.cents());
    }
}
