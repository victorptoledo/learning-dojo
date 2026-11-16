package dojo;

import dojo.OrderExceptions.OrderNotCancellableException;
import dojo.OrderExceptions.OrderNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * The orders, in memory. Written for you. Seeded with 30 orders (ids 1 to 30) of customers
 * cust-1 to cust-3, in every status, created on 2026-03-01 in a shuffled order.
 */
@Component
public class OrderStore {
    private static final OrderStatus[] STATUSES = {
        OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.CANCELLED
    };
    private static final Instant SEED_DAY = Instant.parse("2026-03-01T00:00:00Z");

    private final Map<Long, Order> orders = new ConcurrentSkipListMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public OrderStore() {
        for (int i = 1; i <= 30; i++) {
            long id = nextId.getAndIncrement();
            orders.put(id, new Order(id, "cust-" + (i % 3 + 1), STATUSES[i % 4], 1000 + (i % 5) * 500L,
                    SEED_DAY.plus((i * 11L) % 30, ChronoUnit.HOURS)));
        }
    }

    /** Every order, by id. Filtering, sorting and paging are the API's job. */
    public List<Order> all() {
        return List.copyOf(orders.values());
    }

    public Order find(long id) {
        Order order = orders.get(id);
        if (order == null) {
            throw new OrderNotFoundException(id);
        }
        return order;
    }

    public Order place(OrderRequest request) {
        long id = nextId.getAndIncrement();
        Order order = new Order(id, request.customer(), OrderStatus.PENDING, request.totalCents(),
                Instant.now().truncatedTo(ChronoUnit.SECONDS));
        orders.put(id, order);
        return order;
    }

    /** Cancels a pending or paid order; cancelling a cancelled order changes nothing. */
    public synchronized Order cancel(long id) {
        Order order = find(id);
        return switch (order.status()) {
            case CANCELLED -> order;
            case SHIPPED -> throw new OrderNotCancellableException(id, order.status());
            case PENDING, PAID -> {
                Order cancelled = order.withStatus(OrderStatus.CANCELLED);
                orders.put(id, cancelled);
                yield cancelled;
            }
        };
    }
}
