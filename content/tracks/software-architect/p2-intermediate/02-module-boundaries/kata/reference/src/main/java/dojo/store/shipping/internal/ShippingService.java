package dojo.store.shipping.internal;

import dojo.store.shipping.api.Shipment;
import dojo.store.shipping.api.Shipping;
import java.util.ArrayList;
import java.util.List;

public class ShippingService implements Shipping {
    private final List<Shipment> shipments = new ArrayList<>();

    @Override
    public Shipment ship(String orderId, int weightGrams) {
        Shipment shipment;
        if (weightGrams <= 2000) {
            shipment = new Shipment(orderId, "letter", 450);
        } else if (weightGrams <= 20000) {
            shipment = new Shipment(orderId, "parcel", 900);
        } else {
            shipment = new Shipment(orderId, "freight", 4000);
        }
        shipments.add(shipment);
        return shipment;
    }

    @Override
    public List<Shipment> shipments() {
        return List.copyOf(shipments);
    }
}
