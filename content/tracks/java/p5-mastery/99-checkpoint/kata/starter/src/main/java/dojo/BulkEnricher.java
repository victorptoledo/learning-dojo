package dojo;

/** Takes 300 cents off orders of 10 items or more. Already written. */
public final class BulkEnricher implements Enricher {
    @Override
    public Order enrich(Order order) {
        if (order.quantity() < 10) {
            return order;
        }
        return new Order(order.id(), order.customerId(), order.sku(), order.quantity(),
                Math.max(0, order.priceCents() - 300));
    }
}
