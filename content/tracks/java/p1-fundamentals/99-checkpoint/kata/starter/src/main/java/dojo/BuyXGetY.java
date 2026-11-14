package dojo;

/** For every {@code buy + free} units of {@code sku}, {@code free} of them cost nothing. */
public record BuyXGetY(String sku, int buy, int free) implements Discount {
    public BuyXGetY {
        if (buy < 1 || free < 1) {
            throw new IllegalArgumentException("buy and free must be positive");
        }
    }
}
