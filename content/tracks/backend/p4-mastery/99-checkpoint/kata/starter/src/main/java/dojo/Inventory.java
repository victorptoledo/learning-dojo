package dojo;

/** The inventory service. Every call is idempotent by {@code sagaId}. */
public interface Inventory {
    /** @throws StepFailedException "out of stock" */
    void reserve(String sagaId, String sku, int quantity);

    /** Gives back what {@code sagaId} reserved; nothing if it reserved nothing. */
    void release(String sagaId);
}
