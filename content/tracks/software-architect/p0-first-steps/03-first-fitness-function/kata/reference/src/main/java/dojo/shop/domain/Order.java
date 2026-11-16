package dojo.shop.domain;

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
}
