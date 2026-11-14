package dojo;

import java.time.Duration;

/** Waits between retries. Production code sleeps; tests record the waits instead. */
@FunctionalInterface
public interface Sleeper {
    void sleep(Duration duration);
}
