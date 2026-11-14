package dojo;

import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final OutboxRepository outbox;

    public OrderService(OrderRepository orders, OutboxRepository outbox) {
        this.orders = orders;
        this.outbox = outbox;
    }

    /** Saves the order and its OrderPlaced event together, or neither. */
    public ShopOrder place(NewOrder request) {
        throw new UnsupportedOperationException("todo place " + request);
    }
}
