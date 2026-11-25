package dojo;

import java.util.Map;

/**
 * Counters since the quoter was built: calls actually made to each carrier, failed results
 * and timed-out results. Already written.
 */
public record Stats(Map<String, Long> calls, long failures, long timeouts) {
    public Stats {
        calls = Map.copyOf(calls);
    }
}
