package dojo.events;

/** Places and cancels orders. Today it writes to two systems and hopes. */
public class OrderService {
    private final Database db;
    private final Broker broker;

    public OrderService(Database db, Broker broker) {
        this.db = db;
        this.broker = broker;
    }

    public void place(String orderId, String customerId, long amountCents) {
        db.inTransaction(tx -> {
            if (tx.orders().containsKey(orderId)) {
                throw new IllegalArgumentException("duplicate order " + orderId);
            }
            tx.orders().put(orderId, new OrderRow(orderId, customerId, amountCents, "PLACED"));
            broker.publish(new Event("evt-" + orderId + "-placed", "OrderPlaced", orderId, customerId, amountCents));
            if (amountCents < 1) {
                throw new IllegalArgumentException("bad amount " + amountCents);
            }
            return null;
        });
    }

    public void cancel(String orderId) {
        OrderRow row = db.orders().get(orderId);
        if (row == null) {
            throw new IllegalArgumentException("unknown order " + orderId);
        }
        db.inTransaction(tx -> {
            tx.orders().put(orderId, new OrderRow(orderId, row.customerId(), row.amountCents(), "CANCELLED"));
            return null;
        });
        broker.publish(new Event("evt-" + orderId + "-cancelled", "OrderCancelled", orderId, row.customerId(),
                row.amountCents()));
    }
}
