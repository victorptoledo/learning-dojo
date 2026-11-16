package dojo.shop.orders;

/** One product in an order. */
public record OrderLine(String sku, int quantity, long unitPriceCents) {
    public OrderLine {
        if (quantity <= 0 || unitPriceCents < 0) {
            throw new IllegalArgumentException("bad line for " + sku);
        }
    }
}
