package dojo.shop.infra;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** An in-memory stand-in for the orders table. Do not change it. */
public class OrderTable {
    private final List<OrderRow> rows = new ArrayList<>();

    public void insert(OrderRow row) {
        if (find(row.id()).isPresent()) {
            throw new IllegalArgumentException("duplicate order " + row.id());
        }
        rows.add(row);
    }

    public Optional<OrderRow> find(String id) {
        return rows.stream().filter(row -> row.id().equals(id)).findFirst();
    }

    public List<OrderRow> all() {
        return List.copyOf(rows);
    }
}
