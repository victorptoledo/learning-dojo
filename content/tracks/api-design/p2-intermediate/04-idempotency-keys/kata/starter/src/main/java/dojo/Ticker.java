package dojo;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/** The current time, as a component the tests can move forward. Written for you. */
@Component
public class Ticker {
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-06-01T09:00:00Z"));

    public Instant now() {
        return now.get();
    }

    public void advance(Duration duration) {
        now.updateAndGet(instant -> instant.plus(duration));
    }
}
