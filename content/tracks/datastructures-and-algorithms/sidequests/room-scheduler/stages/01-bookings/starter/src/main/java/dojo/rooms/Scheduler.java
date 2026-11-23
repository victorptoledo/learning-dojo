package dojo.rooms;

import java.util.List;
import java.util.Optional;

/** The bookings of every room. See README.md. */
public final class Scheduler {

    public void addRoom(String room) {
        throw new UnsupportedOperationException("todo");
    }

    public boolean book(String id, String room, long start, long end) {
        throw new UnsupportedOperationException("todo");
    }

    public boolean cancel(String id) {
        throw new UnsupportedOperationException("todo");
    }

    public Optional<Booking> at(String room, long time) {
        throw new UnsupportedOperationException("todo");
    }

    public List<Booking> between(String room, long from, long to) {
        throw new UnsupportedOperationException("todo");
    }

    public int size() {
        throw new UnsupportedOperationException("todo");
    }
}
