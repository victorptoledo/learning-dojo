package dojo;

/** Adds 20% tax to the price, rounded down to the cent. Already written. */
public final class TaxEnricher implements Enricher {
    @Override
    public Order enrich(Order order) {
        return new Order(order.id(), order.customerId(), order.sku(), order.quantity(),
                order.priceCents() + order.priceCents() / 5);
    }
}
