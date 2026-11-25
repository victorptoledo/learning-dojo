package dojo;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Copies customer_name into customer for the rows written before the migration, a batch at a time. */
@Component
public class OrderBackfill {
    private final OrderRepository orders;

    public OrderBackfill(OrderRepository orders) {
        this.orders = orders;
    }

    /** Fills up to {@code batchSize} rows, lowest ids first, and returns how many it changed. */
    @Transactional
    public int runBatch(int batchSize) {
        List<Long> ids = orders.idsWithoutCustomer(PageRequest.of(0, batchSize));
        if (ids.isEmpty()) {
            return 0;
        }
        return orders.copyCustomerName(ids);
    }

    /** How many rows still have no customer: the contract step waits for zero. */
    public long remaining() {
        return orders.countByCustomerIsNull();
    }
}
