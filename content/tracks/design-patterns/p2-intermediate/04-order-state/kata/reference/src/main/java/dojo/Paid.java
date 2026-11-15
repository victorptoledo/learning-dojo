package dojo;

public record Paid(long totalCents) implements OrderState {
    @Override
    public String name() {
        return "paid";
    }

    @Override
    public OrderState ship(String tracking) {
        if (tracking == null || tracking.isBlank()) {
            throw new IllegalArgumentException("a shipment needs a tracking number");
        }
        return new Shipped(tracking);
    }

    @Override
    public OrderState cancel(String reason) {
        return new Cancelled(reason, totalCents);
    }
}
