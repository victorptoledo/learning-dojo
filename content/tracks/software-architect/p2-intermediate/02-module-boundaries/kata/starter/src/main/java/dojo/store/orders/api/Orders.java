package dojo.store.orders.api;

import java.util.Map;

public interface Orders {
    /** Places an order for these quantities by sku, and ships it. */
    OrderConfirmation place(String orderId, Map<String, Integer> quantities);
}
