package dojo.orders;

import java.util.function.Consumer;

/** Where the rules live today: some of them, in some methods. */
public class OrderService {
    private final OrderRepository orders;
    private final Consumer<String> publisher;

    public OrderService(OrderRepository orders, Consumer<String> publisher) {
        this.orders = orders;
        this.publisher = publisher;
    }

    public void addLine(String orderId, String sku, int quantity, long unitPriceCents) {
        Order order = orders.find(orderId).orElseThrow();
        if (!order.getStatus().equals("DRAFT")) {
            throw new IllegalStateException("order is " + order.getStatus());
        }
        for (OrderLine line : order.getLines()) {
            if (line.getSku().equals(sku)) {
                line.setQuantity(line.getQuantity() + quantity);
                order.setTotalCents(order.getTotalCents() + quantity * unitPriceCents);
                return;
            }
        }
        order.getLines().add(new OrderLine(sku, quantity, unitPriceCents));
        order.setTotalCents(order.getTotalCents() + quantity * unitPriceCents);
    }

    public void removeLine(String orderId, String sku) {
        Order order = orders.find(orderId).orElseThrow();
        order.getLines().removeIf(line -> line.getSku().equals(sku));
    }

    public void submit(String orderId) {
        Order order = orders.find(orderId).orElseThrow();
        order.setStatus("SUBMITTED");
        publisher.accept("submitted " + orderId + " for " + order.getCustomer().getName());
    }
}
