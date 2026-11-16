package dojo.orders;

public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("blank customer id");
        }
    }
}
