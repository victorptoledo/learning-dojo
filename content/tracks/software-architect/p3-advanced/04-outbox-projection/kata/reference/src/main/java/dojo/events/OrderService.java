package dojo.events;

/** Places and cancels orders, writing each change and its event in one transaction. */
public class OrderService {
    private final Database db;

    public OrderService(Database db) {
        this.db = db;
    }

    public void place(String orderId, String customerId, long amountCents) {
        if (amountCents < 1) {
            throw new IllegalArgumentException("bad amount " + amountCents);
        }
        db.inTransaction(tx -> {
            if (tx.orders().containsKey(orderId)) {
                throw new IllegalArgumentException("duplicate order " + orderId);
            }
            tx.orders().put(orderId, new OrderRow(orderId, customerId, amountCents, "PLACED"));
            tx.outbox().add(new OutboxMessage(
                    new Event("evt-" + orderId + "-placed", "OrderPlaced", orderId, customerId, amountCents), false));
            return null;
        });
    }

    public void cancel(String orderId) {
        db.inTransaction(tx -> {
            OrderRow row = tx.orders().get(orderId);
            if (row == null) {
                throw new IllegalArgumentException("unknown order " + orderId);
            }
            if (row.status().equals("CANCELLED")) {
                throw new IllegalStateException("order already cancelled " + orderId);
            }
            tx.orders().put(orderId, new OrderRow(orderId, row.customerId(), row.amountCents(), "CANCELLED"));
            tx.outbox().add(new OutboxMessage(new Event("evt-" + orderId + "-cancelled", "OrderCancelled", orderId,
                    row.customerId(), row.amountCents()), false));
            return null;
        });
    }
}
