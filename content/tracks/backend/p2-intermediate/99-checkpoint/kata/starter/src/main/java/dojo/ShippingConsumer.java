package dojo;

import org.springframework.stereotype.Component;

/** Receives OrderPlaced events (at least once!) and creates one shipment per order. */
@Component
public class ShippingConsumer {
    private final ShipmentRepository shipments;
    private final ProcessedEventRepository processed;

    public ShippingConsumer(ShipmentRepository shipments, ProcessedEventRepository processed) {
        this.shipments = shipments;
        this.processed = processed;
    }

    /** Returns true when the event was processed now, false when it was a duplicate. */
    public boolean on(String eventId, String type, String payload) {
        throw new UnsupportedOperationException("todo handle " + eventId);
    }
}
