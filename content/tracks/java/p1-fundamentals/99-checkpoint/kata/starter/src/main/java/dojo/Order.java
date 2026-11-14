package dojo;

import java.util.List;

/** The items of an order; the list cannot change once the order exists. */
public record Order(List<Item> items) {
    public Order {
        throw new UnsupportedOperationException("todo copy " + items);
    }
}
