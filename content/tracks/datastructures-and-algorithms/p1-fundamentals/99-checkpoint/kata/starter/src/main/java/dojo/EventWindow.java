package dojo;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/** Answers questions about the events of the last windowMillis milliseconds in O(1). */
public final class EventWindow {

    public EventWindow(long windowMillis) {
        throw new UnsupportedOperationException("todo window of " + windowMillis + " ms");
    }

    /** Records an event and moves the clock to its timestamp. */
    public void record(long timestamp, String user, int latencyMs) {
        throw new UnsupportedOperationException("todo record " + user);
    }

    /** Moves the clock forward, letting old events leave the window. */
    public void advanceTo(long now) {
        throw new UnsupportedOperationException("todo advanceTo " + now);
    }

    public long now() {
        throw new UnsupportedOperationException("todo now");
    }

    public int count() {
        throw new UnsupportedOperationException("todo count");
    }

    public int distinctUsers() {
        throw new UnsupportedOperationException("todo distinctUsers");
    }

    public int countFor(String user) {
        throw new UnsupportedOperationException("todo countFor " + user);
    }

    public OptionalInt maxLatency() {
        throw new UnsupportedOperationException("todo maxLatency");
    }

    public OptionalDouble averageLatency() {
        throw new UnsupportedOperationException("todo averageLatency");
    }
}
