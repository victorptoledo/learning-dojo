package dojo.tickets;

import java.util.List;

/** An order as the API shows it. */
public record OrderView(long id, long holdId, long eventId, List<String> seats, long totalCents, OrderStatus status) {
    static OrderView of(TicketOrder order) {
        return new OrderView(order.getId(), order.getHoldId(), order.getEventId(), List.copyOf(order.getSeats()),
                order.getTotalCents(), order.getStatus());
    }
}
