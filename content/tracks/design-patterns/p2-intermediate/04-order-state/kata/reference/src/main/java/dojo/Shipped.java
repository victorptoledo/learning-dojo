package dojo;

public record Shipped(String tracking) implements OrderState {
    @Override
    public String name() {
        return "shipped";
    }

    @Override
    public OrderState deliver() {
        return new Delivered(tracking);
    }
}
