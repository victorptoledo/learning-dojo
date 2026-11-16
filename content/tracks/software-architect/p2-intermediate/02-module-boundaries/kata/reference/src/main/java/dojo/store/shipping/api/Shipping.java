package dojo.store.shipping.api;

import java.util.List;

public interface Shipping {
    Shipment ship(String orderId, int weightGrams);

    List<Shipment> shipments();
}
