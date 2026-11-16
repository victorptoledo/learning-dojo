package dojo.orders;

import java.util.ArrayList;
import java.util.List;

/** The aggregate root: every change goes through a method that keeps the invariants. */
public final class Order {
    private static final int MAX_LINES = 10;
    private static final long LIMIT_CENTS = 1_000_000;

    private final OrderId id;
    private final CustomerId customerId;
    private final String currency;
    private OrderStatus status = OrderStatus.DRAFT;
    private List<OrderLine> lines = List.of();
    private final List<DomainEvent> events = new ArrayList<>();

    private Order(OrderId id, CustomerId customerId, String currency) {
        this.id = id;
        this.customerId = customerId;
        this.currency = Money.zero(currency).currency();
    }

    public static Order draft(OrderId id, CustomerId customerId, String currency) {
        return new Order(id, customerId, currency);
    }

    public OrderId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public String currency() {
        return currency;
    }

    public OrderStatus status() {
        return status;
    }

    public List<OrderLine> lines() {
        return List.copyOf(lines);
    }

    public Money total() {
        return totalOf(lines);
    }

    public void addLine(String sku, int quantity, Money unitPrice) {
        requireDraft();
        if (quantity < 1 || !unitPrice.currency().equals(currency)) {
            throw new IllegalArgumentException("bad line for " + sku);
        }
        List<OrderLine> next = new ArrayList<>(lines);
        int at = indexOf(sku);
        if (at >= 0) {
            OrderLine old = next.get(at);
            if (!old.unitPrice().equals(unitPrice)) {
                throw new IllegalArgumentException("price changed for " + sku);
            }
            next.set(at, new OrderLine(sku, old.quantity() + quantity, unitPrice));
        } else if (next.size() == MAX_LINES) {
            throw new IllegalStateException("an order has at most 10 lines");
        } else {
            next.add(new OrderLine(sku, quantity, unitPrice));
        }
        if (totalOf(next).cents() > LIMIT_CENTS) {
            throw new IllegalStateException("order total above limit");
        }
        lines = List.copyOf(next);
    }

    public void removeLine(String sku) {
        requireDraft();
        int at = indexOf(sku);
        if (at < 0) {
            throw new IllegalArgumentException("no line for " + sku);
        }
        List<OrderLine> next = new ArrayList<>(lines);
        next.remove(at);
        lines = List.copyOf(next);
    }

    public void submit() {
        requireDraft();
        if (lines.isEmpty()) {
            throw new IllegalStateException("an empty order cannot be submitted");
        }
        status = OrderStatus.SUBMITTED;
        events.add(new OrderSubmitted(id, customerId, total()));
    }

    public void cancel(String reason) {
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("order is " + status);
        }
        status = OrderStatus.CANCELLED;
        events.add(new OrderCancelled(id, reason));
    }

    public List<DomainEvent> pullEvents() {
        List<DomainEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    private void requireDraft() {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("order is " + status);
        }
    }

    private int indexOf(String sku) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).sku().equals(sku)) {
                return i;
            }
        }
        return -1;
    }

    private Money totalOf(List<OrderLine> candidate) {
        return candidate.stream().map(OrderLine::total).reduce(Money.zero(currency), Money::plus);
    }
}
