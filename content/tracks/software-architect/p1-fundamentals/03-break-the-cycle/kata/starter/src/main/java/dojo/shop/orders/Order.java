package dojo.shop.orders;

import java.util.List;

/** An order and where it is in its life. */
public class Order {
    private final String id;
    private final String customer;
    private final List<OrderLine> lines;
    private OrderStatus status = OrderStatus.PLACED;
    private String invoiceId;

    public Order(String id, String customer, List<OrderLine> lines) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("an order needs at least one line");
        }
        this.id = id;
        this.customer = customer;
        this.lines = List.copyOf(lines);
    }

    public String id() {
        return id;
    }

    public String customer() {
        return customer;
    }

    public List<OrderLine> lines() {
        return lines;
    }

    public OrderStatus status() {
        return status;
    }

    public String invoiceId() {
        return invoiceId;
    }

    public void markInvoiced(String invoiceId) {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException("order " + id + " is " + status);
        }
        this.invoiceId = invoiceId;
        this.status = OrderStatus.INVOICED;
    }

    public void markPaid() {
        if (status != OrderStatus.INVOICED) {
            throw new IllegalStateException("order " + id + " is " + status);
        }
        this.status = OrderStatus.PAID;
    }
}
