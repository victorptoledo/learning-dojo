package dojo.orders;

/** A line inside the order aggregate; only the root creates and replaces them. */
public record OrderLine(String sku, int quantity, Money unitPrice) {
    public Money total() {
        return unitPrice.times(quantity);
    }
}
