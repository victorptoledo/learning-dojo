package dojo;

import org.springframework.stereotype.Component;

/** Copies customer_name into customer for the rows written before the migration, a batch at a time. */
@Component
public class OrderBackfill {
    private final OrderRepository orders;

    public OrderBackfill(OrderRepository orders) {
        this.orders = orders;
    }

    /** Fills up to {@code batchSize} rows, lowest ids first, and returns how many it changed. */
    public int runBatch(int batchSize) {
        throw new UnsupportedOperationException("todo backfill " + batchSize);
    }

    /** How many rows still have no customer: the contract step waits for zero. */
    public long remaining() {
        throw new UnsupportedOperationException("todo remaining");
    }
}
