package dojo;

/** One line of an order. Amounts are in cents. */
public record Item(String sku, String category, long unitCents, int quantity) {
    public Item {
        throw new UnsupportedOperationException("todo validate " + sku);
    }

    public long subtotal() {
        throw new UnsupportedOperationException("todo subtotal of " + sku);
    }
}
