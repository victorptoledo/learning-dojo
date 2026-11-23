package dojo.rooms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Each room keeps its single bookings in a sorted map from start to booking and its series as
 * rules. Every question walks a room as one sorted stream: the singles merged with the infinite
 * stream of each series. Common free time and the agenda merge those streams again with a heap.
 */
public final class Scheduler {

    private static final Comparator<Booking> BY_START_THEN_ROOM = Comparator.comparingLong(Booking::start)
            .thenComparing(Booking::room)
            .thenComparing(Booking::id);

    private final Map<String, Room> rooms = new HashMap<>();
    private final Map<String, Booking> singles = new HashMap<>();
    private final Map<String, Series> series = new HashMap<>();

    public void addRoom(String room) {
        if (room == null || room.isBlank() || rooms.containsKey(room)) {
            throw new IllegalArgumentException("bad or repeated room: " + room);
        }
        rooms.put(room, new Room());
    }

    public boolean book(String id, String room, long start, long end) {
        requireNewId(id);
        if (start >= end) {
            throw new IllegalArgumentException("start " + start + " is not before end " + end);
        }
        Room r = room(room);
        var before = r.singles.floorEntry(start);
        if (before != null && before.getValue().end() > start) {
            return false;
        }
        var after = r.singles.ceilingEntry(start);
        if (after != null && after.getKey() < end) {
            return false;
        }
        for (Series s : r.series) {
            if (s.overlaps(start, end)) {
                return false;
            }
        }
        var booking = new Booking(id, room, start, end);
        r.singles.put(start, booking);
        singles.put(id, booking);
        return true;
    }

    public boolean bookSeries(String id, String room, long firstStart, long duration, long every) {
        requireNewId(id);
        if (duration <= 0 || every < duration) {
            throw new IllegalArgumentException("a series needs 0 < duration <= every: " + duration + ", " + every);
        }
        Room r = room(room);
        var candidate = new Series(id, room, firstStart, duration, every);
        for (Booking single : r.singles.values()) {
            if (candidate.overlaps(single.start(), single.end())) {
                return false;
            }
        }
        for (Series other : r.series) {
            if (candidate.clashes(other)) {
                return false;
            }
        }
        r.series.add(candidate);
        series.put(id, candidate);
        return true;
    }

    public boolean cancel(String id) {
        Booking booking = singles.remove(id);
        if (booking != null) {
            rooms.get(booking.room()).singles.remove(booking.start());
            return true;
        }
        Series gone = series.remove(id);
        if (gone != null) {
            rooms.get(gone.room()).series.remove(gone);
            return true;
        }
        return false;
    }

    public Optional<Booking> at(String room, long time) {
        Iterator<Booking> bookings = room(room).endingAfter(time);
        if (bookings.hasNext()) {
            Booking first = bookings.next();
            if (first.start() <= time) {
                return Optional.of(first);
            }
        }
        return Optional.empty();
    }

    public List<Booking> between(String room, long from, long to) {
        if (from > to) {
            throw new IllegalArgumentException("from " + from + " is after to " + to);
        }
        Room r = room(room);
        var result = new ArrayList<Booking>();
        if (from == to) {
            return result;
        }
        Iterator<Booking> bookings = r.endingAfter(from);
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
        var streams = new ArrayList<Iterator<Booking>>();
        for (String name : rooms) {
            streams.add(room(name).endingAfter(from));
        }
        var merged = new Merge(streams);
        long candidate = from;
        while (candidate + length <= to) {
            if (!merged.hasNext()) {
                return Optional.of(new Slot(candidate, candidate + length));
            }
            Booking first = merged.next();
            if (first.start() >= candidate + length) {
                return Optional.of(new Slot(candidate, candidate + length));
            }
            candidate = Math.max(candidate, first.end());
        }
        return Optional.empty();
    }

    public List<Booking> upcoming(long from, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("negative count " + count);
        }
        var streams = new ArrayList<Iterator<Booking>>();
        for (Room r : rooms.values()) {
            streams.add(r.endingAfter(from));
        }
        var merged = new Merge(streams);
        var result = new ArrayList<Booking>();
        while (result.size() < count && merged.hasNext()) {
            Booking booking = merged.next();
            if (booking.start() >= from) {
                result.add(booking);
            }
        }
        return result;
    }

    public int size() {
        return singles.size() + series.size();
    }

    private void requireNewId(String id) {
        if (id == null || id.isBlank() || id.contains("#") || singles.containsKey(id) || series.containsKey(id)) {
            throw new IllegalArgumentException("bad or repeated id: " + id);
        }
    }

    private Room room(String room) {
        Room r = rooms.get(room);
        if (r == null) {
            throw new IllegalArgumentException("unknown room: " + room);
        }
        return r;
    }

    private static final class Room {
        final TreeMap<Long, Booking> singles = new TreeMap<>();
        final List<Series> series = new ArrayList<>();

        /** Every booking and occurrence that ends after {@code time}, by start. */
        Iterator<Booking> endingAfter(long time) {
            Long first = singles.floorKey(time);
            Iterator<Booking> single = first == null || singles.get(first).end() <= time
                    ? singles.tailMap(time, false).values().iterator()
                    : singles.tailMap(first, true).values().iterator();
            if (series.isEmpty()) {
                return single;
            }
            var streams = new ArrayList<Iterator<Booking>>();
            streams.add(single);
            for (Series s : series) {
                streams.add(s.endingAfter(time));
            }
            return new Merge(streams);
        }
    }

    /** {@code [first + n·every, first + n·every + duration)} for every n >= 0. */
    private record Series(String id, String room, long first, long duration, long every) {

        /** Whether some occurrence overlaps {@code [start, end)}: only the last one before end can. */
        boolean overlaps(long start, long end) {
            if (end <= first) {
                return false;
            }
            long n = Math.floorDiv(end - 1 - first, every);
            return first + n * every + duration > start;
        }

        /**
         * The differences between an occurrence of this and one of {@code other} are exactly the
         * numbers congruent to {@code first - other.first} modulo gcd(every, other.every).
         */
        boolean clashes(Series other) {
            long g = gcd(every, other.every);
            long above = Math.floorMod(first - other.first, g);
            long below = above - g;
            return above < other.duration || below > -duration;
        }

        Iterator<Booking> endingAfter(long time) {
            long start = Math.max(0, Math.floorDiv(time - first - duration, every) + 1);
            return new Iterator<>() {
                long n = start;

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Booking next() {
                    long s = first + n * every;
                    return new Booking(id + "#" + n++, room, s, s + duration);
                }
            };
        }

        private static long gcd(long a, long b) {
            return b == 0 ? a : gcd(b, a % b);
        }
    }

    /** A k-way merge of sorted streams with a min-heap of their next items. */
    private static final class Merge implements Iterator<Booking> {
        private record Head(Booking booking, Iterator<Booking> rest) {
        }

        private final PriorityQueue<Head> heap =
                new PriorityQueue<>(Comparator.comparing(Head::booking, BY_START_THEN_ROOM));

        Merge(List<Iterator<Booking>> streams) {
            for (Iterator<Booking> stream : streams) {
                if (stream.hasNext()) {
                    heap.add(new Head(stream.next(), stream));
                }
            }
        }

        @Override
        public boolean hasNext() {
            return !heap.isEmpty();
        }

        @Override
        public Booking next() {
            Head head = heap.poll();
            if (head == null) {
                throw new NoSuchElementException();
            }
            if (head.rest.hasNext()) {
                heap.add(new Head(head.rest.next(), head.rest));
            }
            return head.booking;
        }
    }
}
