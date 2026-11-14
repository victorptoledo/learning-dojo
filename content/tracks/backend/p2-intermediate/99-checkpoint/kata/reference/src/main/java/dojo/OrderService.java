package dojo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final OutboxRepository outbox;

    public OrderService(OrderRepository orders, OutboxRepository outbox) {
        this.orders = orders;
        this.outbox = outbox;
    }

    /** Saves the order and its OrderPlaced event together, or neither. */
    @Transactional
    public ShopOrder place(NewOrder request) {
        ShopOrder order = orders.save(new ShopOrder(request.customer().strip(), request.totalCents()));
        outbox.save(new OutboxEvent("OrderPlaced",
                "{\"orderId\":%d,\"customer\":\"%s\",\"totalCents\":%d}"
                        .formatted(order.getId(), order.getCustomer(), order.getTotalCents())));
        if (order.getCustomer().equals("blocked")) {
            throw new OrderRefusedException(order.getCustomer());
        }
        return order;
    }
}
