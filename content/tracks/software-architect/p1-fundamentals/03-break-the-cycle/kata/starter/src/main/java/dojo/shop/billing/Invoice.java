package dojo.shop.billing;

/** An invoice for one order. Do not change it. */
public record Invoice(String id, String orderId, String customer, long netCents, long vatCents) {
    public long totalCents() {
        return netCents + vatCents;
    }
}
