package dojo.rooms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Each room keeps its bookings in a sorted map from start to booking; they never overlap, so the
 * map is also sorted by end. Common free time is a lazy k-way merge of the rooms' bookings.
 */
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
        var result = new ArrayList<Booking>();
        if (from == to) {
            room(room);
            return result;
        }
        Iterator<Booking> bookings = endingAfter(room(room), from);
        while (bookings.hasNext()) {
            Booking booking = bookings.next();
            if (booking.start() >= to) {
                break;
            }
            result.add(booking);
        }
        return result;
    }

    public List<Slot> freeSlots(String room, long from, long to) {
        var slots = new ArrayList<Slot>();
        long free = from;
        for (Booking booking : between(room, from, to)) {
            if (booking.start() > free) {
                slots.add(new Slot(free, booking.start()));
            }
            free = Math.max(free, booking.end());
        }
        if (free < to) {
            slots.add(new Slot(free, to));
        }
        return slots;
    }

    public Optional<Slot> firstFreeEverywhere(List<String> rooms, long from, long to, long length) {
        if (rooms == null || rooms.isEmpty() || length <= 0 || from > to) {
            throw new IllegalArgumentException("bad search: " + rooms + " [" + from + ", " + to + ") for " + length);
        }
        var heap = new PriorityQueue<Cursor>(Comparator.comparingLong((Cursor c) -> c.current.start()));
        for (String name : rooms) {
            Iterator<Booking> bookings = endingAfter(room(name), from);
            if (bookings.hasNext()) {
                heap.add(new Cursor(bookings.next(), bookings));
            }
        }
        long candidate = from;
        while (candidate + length <= to) {
            Cursor first = heap.poll();
            if (first == null || first.current.start() >= candidate + length) {
                return Optional.of(new Slot(candidate, candidate + length));
            }
            candidate = Math.max(candidate, first.current.end());
            if (first.rest.hasNext()) {
                first.current = first.rest.next();
                heap.add(first);
            }
        }
        return Optional.empty();
    }

    public int size() {
        return byId.size();
    }

    /** The next booking of one room in the merge, and the ones after it. */
    private static final class Cursor {
        Booking current;
        final Iterator<Booking> rest;

        Cursor(Booking current, Iterator<Booking> rest) {
            this.current = current;
            this.rest = rest;
        }
    }

    /** The bookings that end after {@code time}, by start: the one holding it, then the later ones. */
    private static Iterator<Booking> endingAfter(TreeMap<Long, Booking> bookings, long time) {
        Long first = bookings.floorKey(time);
        if (first == null || bookings.get(first).end() <= time) {
            return bookings.tailMap(time, false).values().iterator();
        }
        return bookings.tailMap(first, true).values().iterator();
    }

    private TreeMap<Long, Booking> room(String room) {
        TreeMap<Long, Booking> bookings = rooms.get(room);
        if (bookings == null) {
            throw new IllegalArgumentException("unknown room: " + room);
        }
        return bookings;
    }
}
