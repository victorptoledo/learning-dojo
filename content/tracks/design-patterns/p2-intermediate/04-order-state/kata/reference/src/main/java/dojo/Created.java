package dojo;

public record Created(long totalCents) implements OrderState {
    @Override
    public String name() {
        return "created";
    }

    @Override
    public OrderState pay() {
        return new Paid(totalCents);
    }

    @Override
    public OrderState cancel(String reason) {
        return new Cancelled(reason, 0);
    }
}
