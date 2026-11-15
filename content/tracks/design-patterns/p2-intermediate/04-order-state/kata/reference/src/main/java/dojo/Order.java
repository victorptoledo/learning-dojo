package dojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** An order delegates every transition to its current state and keeps the states it went through. */
public class Order {
    private final String id;
    private OrderState state;
    private final List<OrderState> history = new ArrayList<>();

    public Order(String id, long totalCents) {
        this.id = id;
        moveTo(new Created(totalCents));
    }

    public void pay() {
        moveTo(state.pay());
    }

    public void ship(String tracking) {
        moveTo(state.ship(tracking));
    }

    public void deliver() {
        moveTo(state.deliver());
    }

    public void cancel(String reason) {
        moveTo(state.cancel(reason));
    }

    public OrderState state() {
        return state;
    }

    public List<OrderState> history() {
        return List.copyOf(history);
    }

    public String status() {
        return state.name().toUpperCase(Locale.ROOT);
    }

    public String describe() {
        return id + ": " + switch (state) {
            case Created created -> "created";
            case Paid paid -> "paid " + money(paid.totalCents());
            case Shipped shipped -> "shipped with " + shipped.tracking();
            case Delivered delivered -> "delivered with " + delivered.tracking();
            case Cancelled cancelled -> "cancelled (" + cancelled.reason() + "), refunded " + money(cancelled.refundedCents());
        };
    }

    private void moveTo(OrderState next) {
        state = next;
        history.add(next);
    }

    private static String money(long cents) {
        return "%d.%02d".formatted(cents / 100, cents % 100);
    }
}
