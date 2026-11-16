package dojo;

import java.time.Instant;

/** An order. Totals are in cents. */
public record Order(long id, String customer, OrderStatus status, long totalCents, Instant createdAt) {

    Order withStatus(OrderStatus newStatus) {
        return new Order(id, customer, newStatus, totalCents, createdAt);
    }
}
