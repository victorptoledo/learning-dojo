package dojo.rooms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Each room keeps its bookings in a sorted map from start to booking; they never overlap. */
public final class Scheduler {

    private final Map<String, TreeMap<Long, Booking>> rooms = new HashMap<>();
    private final Map<String, Booking> byId = new HashMap<>();

    public void addRoom(String room) {
        if (room == null || room.isBlank() || rooms.containsKey(room)) {
            throw new IllegalArgumentException("bad or repeated room: " + room);
        }
        rooms.put(room, new TreeMap<>());
    }

    public boolean book(String id, String room, long start, long end) {
        if (id == null || id.isBlank() || byId.containsKey(id)) {
            throw new IllegalArgumentException("bad or repeated id: " + id);
        }
        if (start >= end) {
            throw new IllegalArgumentException("start " + start + " is not before end " + end);
        }
        TreeMap<Long, Booking> bookings = room(room);
        var before = bookings.floorEntry(start);
        if (before != null && before.getValue().end() > start) {
            return false;
        }
        var after = bookings.ceilingEntry(start);
        if (after != null && after.getKey() < end) {
            return false;
        }
        var booking = new Booking(id, room, start, end);
        bookings.put(start, booking);
        byId.put(id, booking);
        return true;
    }

    public boolean cancel(String id) {
        Booking booking = byId.remove(id);
        if (booking == null) {
            return false;
        }
        rooms.get(booking.room()).remove(booking.start());
        return true;
    }

    public Optional<Booking> at(String room, long time) {
        var entry = room(room).floorEntry(time);
        if (entry == null || entry.getValue().end() <= time) {
            return Optional.empty();
        }
        return Optional.of(entry.getValue());
    }

    public List<Booking> between(String room, long from, long to) {
        if (from > to) {
            throw new IllegalArgumentException("from " + from + " is after to " + to);
        }
        TreeMap<Long, Booking> bookings = room(room);
        var result = new ArrayList<Booking>();
        if (from == to) {
            return result;
        }
        Long first = bookings.floorKey(from);
        if (first == null || bookings.get(first).end() <= from) {
            first = from;
        }
        for (Booking booking : bookings.subMap(first, true, to, false).values()) {
            result.add(booking);
        }
        return result;
    }

    public int size() {
        return byId.size();
    }

    private TreeMap<Long, Booking> room(String room) {
        TreeMap<Long, Booking> bookings = rooms.get(room);
        if (bookings == null) {
            throw new IllegalArgumentException("unknown room: " + room);
        }
        return bookings;
    }
}
