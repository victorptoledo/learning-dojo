package dojo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

/**
 * The outbound HTTP client that POSTs webhooks to each partner's registered endpoint. Written
 * for you, as a fake: it records every delivery, and the tests can make a partner's endpoint
 * answer 503 for its next few deliveries.
 */
@Component
public class WebhookSender {
    /** One attempt to deliver a webhook, and the status the partner's endpoint answered. */
    public record Delivery(String partnerId, Map<String, String> headers, String body, int status) {}

    private final List<Delivery> deliveries = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> failures = new ConcurrentHashMap<>();

    /** POSTs the body with these headers to the partner's endpoint and returns its status. */
    public int deliver(String partnerId, Map<String, String> headers, String body) {
        int[] status = {204};
        failures.computeIfPresent(partnerId, (id, left) -> {
            status[0] = 503;
            return left > 1 ? left - 1 : null;
        });
        deliveries.add(new Delivery(partnerId, Map.copyOf(headers), body, status[0]));
        return status[0];
    }

    /** Makes the partner's endpoint answer 503 to its next {@code times} deliveries. */
    public void failNext(String partnerId, int times) {
        failures.put(partnerId, times);
    }

    /** Every delivery attempt to this partner, in order. */
    public List<Delivery> deliveriesTo(String partnerId) {
        return deliveries.stream().filter(delivery -> delivery.partnerId().equals(partnerId)).toList();
    }
}
