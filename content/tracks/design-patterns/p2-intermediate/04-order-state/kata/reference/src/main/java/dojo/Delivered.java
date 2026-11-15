package dojo;

public record Delivered(String tracking) implements OrderState {
    @Override
    public String name() {
        return "delivered";
    }
}
