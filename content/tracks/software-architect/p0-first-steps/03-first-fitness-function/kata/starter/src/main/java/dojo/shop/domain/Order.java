package dojo.shop.domain;

import dojo.shop.infra.OrderRow;
import java.util.List;

/** An order in the currency the customer pays in. */
public record Order(String id, String currency, List<OrderLine> lines) {
    public Order {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("an order needs at least one line");
        }
        lines = List.copyOf(lines);
    }

    public long totalCents() {
        return lines.stream().mapToLong(OrderLine::totalCents).sum();
    }

    /** The row the order table stores. */
    public OrderRow toRow(long totalEuroCents) {
        return new OrderRow(id, lines.size(), totalEuroCents);
    }
}
