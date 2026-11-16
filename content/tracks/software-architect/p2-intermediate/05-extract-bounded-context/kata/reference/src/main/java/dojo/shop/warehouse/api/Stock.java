package dojo.shop.warehouse.api;

/** What the warehouse publishes for its customers. */
public interface Stock {
    /** Takes {@code quantity} off the shelf, or returns false and changes nothing. */
    boolean reserve(String sku, int quantity);
}
