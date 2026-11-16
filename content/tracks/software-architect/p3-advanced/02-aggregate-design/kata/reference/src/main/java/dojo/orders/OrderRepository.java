package dojo.orders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** One repository per aggregate, keyed by the root's identity. */
public class OrderRepository {
    private final Map<OrderId, Order> orders = new HashMap<>();

    public void save(Order order) {
        orders.put(order.id(), order);
    }

    public Optional<Order> find(OrderId id) {
        return Optional.ofNullable(orders.get(id));
    }
}
