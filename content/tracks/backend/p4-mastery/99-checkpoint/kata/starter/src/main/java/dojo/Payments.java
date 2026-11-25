package dojo;

/** The payment service. Every call is idempotent by {@code sagaId}. */
public interface Payments {
    /** @throws StepFailedException "card declined" */
    void charge(String sagaId, long cents);

    /** Refunds what {@code sagaId} charged; nothing if it charged nothing. */
    void refund(String sagaId);
}
