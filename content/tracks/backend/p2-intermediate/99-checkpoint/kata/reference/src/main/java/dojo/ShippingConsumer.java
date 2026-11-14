package dojo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Receives OrderPlaced events (at least once!) and creates one shipment per order. */
@Component
public class ShippingConsumer {
    private static final Pattern ORDER_ID = Pattern.compile("\"orderId\":(\\d+)");
    private final ShipmentRepository shipments;
    private final ProcessedEventRepository processed;
    private final TransactionTemplate transaction;

    public ShippingConsumer(ShipmentRepository shipments, ProcessedEventRepository processed,
                            TransactionTemplate transaction) {
        this.shipments = shipments;
        this.processed = processed;
        this.transaction = transaction;
    }

    /** Returns true when the event was processed now, false when it was a duplicate. */
    public boolean on(String eventId, String type, String payload) {
        if (!type.equals("OrderPlaced")) {
            return false;
        }
        Matcher matcher = ORDER_ID.matcher(payload);
        if (!matcher.find()) {
            throw new IllegalArgumentException("no orderId in " + payload);
        }
        long orderId = Long.parseLong(matcher.group(1));
        try {
            return Boolean.TRUE.equals(transaction.execute(status -> {
                processed.saveAndFlush(new ProcessedEvent(eventId));
                shipments.save(new Shipment(orderId));
                return true;
            }));
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }
}
