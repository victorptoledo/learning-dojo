package dojo;

/** One line of an order. Amounts are in cents. */
public record Item(String sku, String category, long unitCents, int quantity) {
    public Item {
        if (sku == null || sku.isBlank() || category == null || category.isBlank()) {
            throw new IllegalArgumentException("sku and category are required");
        }
        if (unitCents < 0 || quantity < 1) {
            throw new IllegalArgumentException("invalid price or quantity for " + sku);
        }
        sku = sku.strip();
        category = category.strip();
    }

    public long subtotal() {
        return unitCents * quantity;
    }
}
