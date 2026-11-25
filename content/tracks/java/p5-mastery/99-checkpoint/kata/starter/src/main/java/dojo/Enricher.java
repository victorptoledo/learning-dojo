package dojo;

/** One step that adjusts an order before it is stored. Already written. */
public interface Enricher {
    Order enrich(Order order);
}
