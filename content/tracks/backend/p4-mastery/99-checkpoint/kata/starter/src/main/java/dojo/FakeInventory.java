package dojo;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** The inventory service for the kata. It lives outside the process, so a crash does not reset it. */
@Component
public class FakeInventory implements Inventory {
    private record Reservation(String sku, int quantity) {}

    private final Map<String, Integer> stock = new HashMap<>(Map.of("BOOK", 100, "LAMP", 2));
    private final Map<String, Reservation> reservations = new HashMap<>();
    private boolean crashOnNextRelease;

    @Override
    public synchronized void reserve(String sagaId, String sku, int quantity) {
        if (reservations.containsKey(sagaId)) {
            return;
        }
        int available = stock.getOrDefault(sku, 0);
        if (available < quantity) {
            throw new StepFailedException("out of stock");
        }
        stock.put(sku, available - quantity);
        reservations.put(sagaId, new Reservation(sku, quantity));
    }

    @Override
    public synchronized void release(String sagaId) {
        if (crashOnNextRelease) {
            crashOnNextRelease = false;
            throw new CrashException("the process died while releasing the stock");
        }
        Reservation reservation = reservations.remove(sagaId);
        if (reservation != null) {
            stock.merge(reservation.sku(), reservation.quantity(), Integer::sum);
        }
    }

    public synchronized int stock(String sku) {
        return stock.getOrDefault(sku, 0);
    }

    public synchronized boolean reserved(String sagaId) {
        return reservations.containsKey(sagaId);
    }

    public synchronized void crashOnNextRelease() {
        crashOnNextRelease = true;
    }
}
