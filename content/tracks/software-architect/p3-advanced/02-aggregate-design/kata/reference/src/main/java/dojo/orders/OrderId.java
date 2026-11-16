package dojo.orders;

public record OrderId(String value) {
    public OrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("blank order id");
        }
    }
}
