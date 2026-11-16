package dojo.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A tiny transactional database with two tables. {@code inTransaction} works on copies and
 * commits them only if the work returns normally: an exception throws every change away.
 * Do not change it.
 */
public final class Database {
    private Map<String, OrderRow> orders = new LinkedHashMap<>();
    private List<OutboxMessage> outbox = new ArrayList<>();

    public <T> T inTransaction(Function<Transaction, T> work) {
        Transaction tx = new Transaction(new LinkedHashMap<>(orders), new ArrayList<>(outbox));
        T result = work.apply(tx);
        orders = tx.orders();
        outbox = tx.outbox();
        return result;
    }

    /** The committed orders. */
    public Map<String, OrderRow> orders() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(orders));
    }

    /** The committed outbox, oldest first. */
    public List<OutboxMessage> outbox() {
        return List.copyOf(outbox);
    }
}
