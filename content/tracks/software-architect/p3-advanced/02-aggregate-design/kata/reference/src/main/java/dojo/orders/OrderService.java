package dojo.orders;

import java.util.function.Consumer;

/** The application service: load, ask the aggregate, save, publish. No rules of its own. */
public class OrderService {
    private final OrderRepository orders;
    private final Consumer<DomainEvent> publisher;

    public OrderService(OrderRepository orders, Consumer<DomainEvent> publisher) {
        this.orders = orders;
        this.publisher = publisher;
    }

    public OrderId create(String orderId, String customerId, String currency) {
        Order order = Order.draft(new OrderId(orderId), new CustomerId(customerId), currency);
        save(order);
        return order.id();
    }

    public void addLine(OrderId id, String sku, int quantity, Money unitPrice) {
        Order order = load(id);
        order.addLine(sku, quantity, unitPrice);
        save(order);
    }

    public void submit(OrderId id) {
        Order order = load(id);
        order.submit();
        save(order);
    }

    public void cancel(OrderId id, String reason) {
        Order order = load(id);
        order.cancel(reason);
        save(order);
    }

    private Order load(OrderId id) {
        return orders.find(id).orElseThrow(() -> new IllegalArgumentException("unknown order " + id.value()));
    }

    private void save(Order order) {
        orders.save(order);
        order.pullEvents().forEach(publisher);
    }
}
