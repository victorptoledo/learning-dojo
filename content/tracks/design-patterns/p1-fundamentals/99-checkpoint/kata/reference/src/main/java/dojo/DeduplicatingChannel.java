package dojo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Decorator: drops a message identical to one delivered less than {@code window} ago.
 * Only successful deliveries count, so a failed send can be retried at once.
 */
public final class DeduplicatingChannel implements Channel {
    private final Channel inner;
    private final Clock clock;
    private final Duration window;
    private final Map<Message, Instant> lastDelivered = new HashMap<>();

    public DeduplicatingChannel(Channel inner, Clock clock, Duration window) {
        if (window.isNegative() || window.isZero()) throw new IllegalArgumentException("window must be positive");
        this.inner = inner;
        this.clock = clock;
        this.window = window;
    }

    @Override
    public void deliver(Message message) {
        Instant now = clock.instant();
        Instant last = lastDelivered.get(message);
        if (last != null && Duration.between(last, now).compareTo(window) < 0) {
            return;
        }
        inner.deliver(message);
        lastDelivered.put(message, now);
    }
}
