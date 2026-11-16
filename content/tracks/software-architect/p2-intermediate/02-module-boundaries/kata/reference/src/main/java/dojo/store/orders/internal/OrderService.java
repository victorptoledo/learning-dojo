package dojo.store.orders.internal;

import dojo.store.catalog.api.Catalog;
import dojo.store.catalog.api.ProductView;
import dojo.store.orders.api.OrderConfirmation;
import dojo.store.orders.api.Orders;
import dojo.store.shipping.api.Shipment;
import dojo.store.shipping.api.Shipping;
import java.util.Map;

public class OrderService implements Orders {
    private final Catalog catalog;
    private final Shipping shipping;

    public OrderService(Catalog catalog, Shipping shipping) {
        this.catalog = catalog;
        this.shipping = shipping;
    }

    @Override
    public OrderConfirmation place(String orderId, Map<String, Integer> quantities) {
        long goods = 0;
        int weight = 0;
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            ProductView product = catalog.product(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("unknown sku " + entry.getKey()));
            if (entry.getValue() <= 0) {
                throw new IllegalArgumentException("bad quantity for " + entry.getKey());
            }
            goods += product.priceCents() * entry.getValue();
            weight += product.weightGrams() * entry.getValue();
        }
        Shipment shipment = shipping.ship(orderId, weight);
        return new OrderConfirmation(orderId, goods, shipment.carrier(), shipment.costCents());
    }
}
