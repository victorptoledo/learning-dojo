package dojo;

/** The shipping service. Every call is idempotent by {@code sagaId}. */
public interface Shipping {
    /** @throws StepFailedException "undeliverable" */
    void schedule(String sagaId, String address);
}
