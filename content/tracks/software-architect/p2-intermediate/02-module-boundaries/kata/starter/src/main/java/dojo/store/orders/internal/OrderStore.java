package dojo.store.orders.internal;

import java.util.HashMap;
import java.util.Map;

/** Remembers how heavy each order is. */
public class OrderStore {
    private final Map<String, Integer> weights = new HashMap<>();

    public void record(String orderId, int weightGrams) {
        weights.put(orderId, weightGrams);
    }

    public int weightOf(String orderId) {
        Integer weight = weights.get(orderId);
        if (weight == null) {
            throw new IllegalArgumentException("unknown order " + orderId);
        }
        return weight;
    }
}
