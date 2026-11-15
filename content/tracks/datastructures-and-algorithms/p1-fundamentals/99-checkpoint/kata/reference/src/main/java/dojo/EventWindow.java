package dojo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/** Answers questions about the events of the last windowMillis milliseconds in O(1). */
public final class EventWindow {
    private record Event(long timestamp, String user, int latency) {}

    private final long windowMillis;
    private final Deque<Event> events = new ArrayDeque<>();
    private final Deque<Event> maxima = new ArrayDeque<>();
    private final Map<String, Integer> perUser = new HashMap<>();
    private long latencySum;
    private long now;

    public EventWindow(long windowMillis) {
        if (windowMillis < 1) throw new IllegalArgumentException("the window must be at least 1 ms");
        this.windowMillis = windowMillis;
    }

    public void record(long timestamp, String user, int latencyMs) {
        if (user == null || user.isBlank()) throw new IllegalArgumentException("a user is required");
        if (latencyMs < 0) throw new IllegalArgumentException("latency cannot be negative");
        advanceTo(timestamp);
        var event = new Event(timestamp, user, latencyMs);
        events.addLast(event);
        perUser.merge(user, 1, Integer::sum);
        latencySum += latencyMs;
        while (!maxima.isEmpty() && maxima.peekLast().latency() < latencyMs) {
            maxima.pollLast();
        }
        maxima.addLast(event);
    }

    public void advanceTo(long now) {
        if (now < this.now) throw new IllegalArgumentException("time cannot go back: " + now + " < " + this.now);
        this.now = now;
        while (!events.isEmpty() && events.peekFirst().timestamp() <= now - windowMillis) {
            Event gone = events.pollFirst();
            perUser.computeIfPresent(gone.user(), (user, count) -> count == 1 ? null : count - 1);
            latencySum -= gone.latency();
            if (maxima.peekFirst() == gone) {
                maxima.pollFirst();
            }
        }
    }

    public long now() {
        return now;
    }

    public int count() {
        return events.size();
    }

    public int distinctUsers() {
        return perUser.size();
    }

    public int countFor(String user) {
        return perUser.getOrDefault(user, 0);
    }

    public OptionalInt maxLatency() {
        return maxima.isEmpty() ? OptionalInt.empty() : OptionalInt.of(maxima.peekFirst().latency());
    }

    public OptionalDouble averageLatency() {
        return events.isEmpty() ? OptionalDouble.empty() : OptionalDouble.of((double) latencySum / events.size());
    }
}
