package dojo.shop.orders;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** An in-memory store of orders. */
public class OrderRepository {
    private final Map<String, Order> orders = new LinkedHashMap<>();

    public void save(Order order) {
        orders.put(order.id(), order);
    }

    public Optional<Order> find(String id) {
        return Optional.ofNullable(orders.get(id));
    }
}
