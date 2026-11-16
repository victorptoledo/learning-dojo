package dojo.store.orders.internal;

import dojo.store.catalog.internal.ProductRow;
import dojo.store.catalog.internal.ProductTable;
import dojo.store.orders.api.OrderConfirmation;
import dojo.store.orders.api.Orders;
import dojo.store.shipping.api.Shipment;
import dojo.store.shipping.internal.ShippingService;
import java.util.Map;

public class OrderService implements Orders {
    private final ProductTable products;
    private final OrderStore store;
    private final ShippingService shipping;

    public OrderService(ProductTable products, OrderStore store, ShippingService shipping) {
        this.products = products;
        this.store = store;
        this.shipping = shipping;
    }

    @Override
    public OrderConfirmation place(String orderId, Map<String, Integer> quantities) {
        long goods = 0;
        int weight = 0;
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            ProductRow row = products.find(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("unknown sku " + entry.getKey()));
            if (entry.getValue() <= 0) {
                throw new IllegalArgumentException("bad quantity for " + entry.getKey());
            }
            goods += row.priceCents() * entry.getValue();
            weight += row.weightGrams() * entry.getValue();
        }
        store.record(orderId, weight);
        Shipment shipment = shipping.ship(orderId);
        return new OrderConfirmation(orderId, goods, shipment.carrier(), shipment.costCents());
    }
}
