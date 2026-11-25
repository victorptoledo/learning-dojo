package dojo;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** The shipping service for the kata. Addresses in Atlantis are undeliverable. */
@Component
public class FakeShipping implements Shipping {
    private final Map<String, String> scheduled = new HashMap<>();

    @Override
    public synchronized void schedule(String sagaId, String address) {
        if (address.contains("Atlantis")) {
            throw new StepFailedException("undeliverable");
        }
        scheduled.putIfAbsent(sagaId, address);
    }

    public synchronized int scheduledCount() {
        return scheduled.size();
    }

    public synchronized boolean scheduled(String sagaId) {
        return scheduled.containsKey(sagaId);
    }
}
