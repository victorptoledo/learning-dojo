package dojo;

public record Cancelled(String reason, long refundedCents) implements OrderState {
    @Override
    public String name() {
        return "cancelled";
    }
}
