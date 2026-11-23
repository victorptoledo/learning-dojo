package dojo.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class Stage4RecurringSeriesTest {

    private static final long DAY = 1_440;
    private static final long WEEK = 7 * DAY;

    private static List<String> ids(List<Booking> bookings) {
        return bookings.stream().map(Booking::id).toList();
    }

    private static Scheduler office() {
        var s = new Scheduler();
        s.addRoom("orion");
        s.addRoom("lyra");
        return s;
    }

    @Test
    void theReadmeExample() {
        var s = office();
        assertTrue(s.bookSeries("standup", "orion", 540, 15, DAY));
        assertTrue(s.bookSeries("review", "orion", 600, 60, WEEK));
        assertFalse(s.book("lunch", "orion", DAY + 540, DAY + 600));
        assertEquals("standup#3", s.at("orion", 3 * DAY + 545).orElseThrow().id());
        assertEquals(List.of("standup#0", "review#0", "standup#1"), ids(s.upcoming(0, 3)));
        assertEquals(2, s.size());
    }

    @Test
    void occurrencesLookLikeBookings() {
        var s = office();
        s.bookSeries("standup", "orion", 540, 15, DAY);
        s.book("lunch", "orion", 720, 780);
        assertEquals(Optional.of(new Booking("standup#10", "orion", 10 * DAY + 540, 10 * DAY + 555)),
                s.at("orion", 10 * DAY + 540));
        assertEquals(Optional.empty(), s.at("orion", 10 * DAY + 555));
        assertEquals(Optional.empty(), s.at("orion", 539), "nothing before the first occurrence");
        assertEquals(List.of("standup#0", "lunch", "standup#1"), ids(s.between("orion", 0, DAY + 541)));
        assertEquals(List.of(new Slot(555, 720), new Slot(780, DAY + 540)), s.freeSlots("orion", 545, DAY + 545));
        assertEquals(Optional.of(new Slot(5 * DAY + 555, 5 * DAY + 615)),
                s.firstFreeEverywhere(List.of("orion", "lyra"), 5 * DAY + 530, 6 * DAY, 60));
    }

    @Test
    void aSeriesIsRefusedWhenAnyOccurrenceWouldClash() {
        var s = office();
        assertTrue(s.book("far-future", "orion", 1_000 * DAY + 545, 1_000 * DAY + 546));
        assertFalse(s.bookSeries("standup", "orion", 540, 15, DAY), "occurrence 1,000 meets a single booking");
        assertTrue(s.bookSeries("standup", "lyra", 540, 15, DAY));
        assertFalse(s.bookSeries("other", "lyra", 554 + 5 * DAY, 30, WEEK), "overlaps day 5's stand-up");
        assertTrue(s.bookSeries("other", "lyra", 555 + 5 * DAY, 30, WEEK), "starts as day 5's stand-up ends");
        assertFalse(s.book("x", "lyra", 26 * DAY + 560, 26 * DAY + 561), "week 3 of other");
        assertTrue(s.book("y", "lyra", 27 * DAY + 560, 27 * DAY + 561));
        assertEquals(4, s.size(), "far-future, standup, other and y");
    }

    @Test
    void seriesWithDifferentPeriodsMeetByTheirGcd() {
        var s = office();
        // every 6 and every 10 minutes: starts differ by 2 mod gcd 2 = 0, so they meet (at minute 12 and 12)
        assertTrue(s.bookSeries("six", "orion", 0, 1, 6));
        assertFalse(s.bookSeries("ten", "orion", 2, 1, 10));
        // odd starts never meet even starts when both periods are even
        assertTrue(s.bookSeries("ten-odd", "orion", 3, 1, 10));
        // 9 and 6: gcd 3; "six" covers residue 0 mod 3, a start at 4 is residue 1, length 2 reaches residue 2
        assertTrue(s.bookSeries("nine", "lyra", 4, 2, 9));
        assertFalse(s.bookSeries("six-b", "lyra", 0, 2, 6), "differences of 4 - 0 mod 3 = 1 < 2");
        assertTrue(s.bookSeries("six-c", "lyra", 0, 1, 6), "a length of 1 at residue 0 misses 1 and 2");
    }

    @Test
    void cancellingASeriesRemovesEveryOccurrence() {
        var s = office();
        s.bookSeries("standup", "orion", 540, 15, DAY);
        assertFalse(s.cancel("standup#2"), "an occurrence is not cancelled alone");
        assertTrue(s.cancel("standup"));
        assertEquals(Optional.empty(), s.at("orion", 2 * DAY + 545));
        assertEquals(0, s.size());
        assertTrue(s.book("standup", "orion", 540, 555), "the id is free again");
    }

    @Test
    void badSeriesAreRefused() {
        var s = office();
        s.bookSeries("standup", "orion", 540, 15, DAY);
        assertThrows(IllegalArgumentException.class, () -> s.bookSeries("standup", "lyra", 0, 1, 10), "repeated id");
        assertThrows(IllegalArgumentException.class, () -> s.book("standup", "lyra", 0, 1), "a series id is taken too");
        assertThrows(IllegalArgumentException.class, () -> s.bookSeries("a#1", "lyra", 0, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> s.book("a#1", "lyra", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> s.bookSeries("a", "lyra", 0, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> s.bookSeries("a", "lyra", 0, 11, 10));
        assertThrows(IllegalArgumentException.class, () -> s.bookSeries("a", "nope", 0, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> s.upcoming(0, -1));
        assertTrue(s.bookSeries("back-to-back", "lyra", 0, 10, 10), "every == duration is allowed");
        assertEquals(List.of(), s.upcoming(0, 0));
    }

    @Test
    void upcomingMergesEveryRoomTiesByRoomName() {
        var s = office();
        s.addRoom("andromeda");
        s.bookSeries("a", "orion", 100, 10, 100);
        s.bookSeries("b", "lyra", 100, 10, 200);
        s.book("c", "andromeda", 100, 400);
        s.book("d", "andromeda", 400, 401);
        // andromeda < lyra < orion when they start together
        assertEquals(List.of("c", "b#0", "a#0", "a#1", "b#1", "a#2", "d", "a#3", "b#2"), ids(s.upcoming(50, 9)));
        assertEquals(List.of("b#1", "a#2", "d"), ids(s.upcoming(300, 3)), "c started before 300: not upcoming");
        var single = new Scheduler();
        single.addRoom("x");
        single.book("only", "x", 5, 6);
        assertEquals(List.of("only"), ids(single.upcoming(0, 10)), "fewer than count");
    }

    /** Every booking and occurrence of the scheduler's rooms that starts before the horizon. */
    private record Model(List<Booking> singles, List<long[]> series, List<String> seriesIds, List<String> seriesRooms) {

        List<Booking> expand(long horizon) {
            var all = new ArrayList<>(singles);
            for (int i = 0; i < series.size(); i++) {
                long[] r = series.get(i);
                for (long n = 0; r[0] + n * r[2] < horizon; n++) {
                    long start = r[0] + n * r[2];
                    all.add(new Booking(seriesIds.get(i) + "#" + n, seriesRooms.get(i), start, start + r[1]));
                }
            }
            all.sort(Comparator.comparingLong(Booking::start).thenComparing(Booking::room));
            return all;
        }

        boolean freeFor(String room, List<Booking> candidates) {
            boolean[] busy = new boolean[4_100];
            for (Booking b : expand(4_000)) {
                if (b.room().equals(room)) {
                    for (long t = b.start(); t < b.end(); t++) {
                        busy[(int) t] = true;
                    }
                }
            }
            for (Booking c : candidates) {
                for (long t = c.start(); t < c.end(); t++) {
                    if (busy[(int) t]) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    @Test
    void randomCalendarsMatchTheExpandedOccurrences() {
        var random = new Random(51);
        for (int round = 0; round < 60; round++) {
            var s = new Scheduler();
            var model = new Model(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            List<String> names = List.of("r0", "r1");
            names.forEach(s::addRoom);
            for (int step = 0; step < 12; step++) {
                String room = names.get(random.nextInt(2));
                String id = "x" + step;
                if (random.nextBoolean()) {
                    long first = random.nextInt(200);
                    long every = 5 + random.nextInt(40);
                    long duration = 1 + random.nextInt((int) Math.min(every, 8));
                    // periods below 45 repeat their pattern within 45 * 44 minutes, inside the 4,000 expanded
                    var occurrences = new ArrayList<Booking>();
                    for (long n = 0; first + n * every < 3_600; n++) {
                        occurrences.add(new Booking(id, room, first + n * every, first + n * every + duration));
                    }
                    boolean free = model.freeFor(room, occurrences);
                    assertEquals(free, s.bookSeries(id, room, first, duration, every),
                            "series " + id + " in " + room + " from " + first + " for " + duration + " every " + every);
                    if (free) {
                        model.series().add(new long[] {first, duration, every});
                        model.seriesIds().add(id);
                        model.seriesRooms().add(room);
                    }
                } else {
                    long start = random.nextInt(1_500);
                    long end = start + 1 + random.nextInt(10);
                    boolean free = model.freeFor(room, List.of(new Booking(id, room, start, end)));
                    assertEquals(free, s.book(id, room, start, end), "booking " + id + " [" + start + ", " + end + ")");
                    if (free) {
                        model.singles().add(new Booking(id, room, start, end));
                    }
                }
            }
            List<Booking> all = model.expand(3_000);
            for (int query = 0; query < 10; query++) {
                long from = random.nextInt(1_500);
                long to = from + random.nextInt(300);
                for (String room : names) {
                    List<Booking> expected = all.stream()
                            .filter(b -> b.room().equals(room) && b.start() < to && from < b.end())
                            .toList();
                    if (from == to) {
                        expected = List.of();
                    }
                    assertEquals(expected, s.between(room, from, to), "between " + from + " and " + to + " in " + room);
                }
                int count = random.nextInt(15);
                List<Booking> next = all.stream().filter(b -> b.start() >= from).limit(count).toList();
                assertEquals(next, s.upcoming(from, count), "upcoming " + count + " from " + from);
            }
        }
    }

    @Test
    void theAgendaOfABusyBuildingIsAMerge() {
        var s = new Scheduler();
        var names = new ArrayList<String>();
        for (int r = 0; r < 500; r++) {
            String name = "room" + r;
            names.add(name);
            s.addRoom(name);
            for (int k = 0; k < 4; k++) {
                assertTrue(s.bookSeries(name + "-s" + k, name, k * 15L + r % 5, 10, 60L * (1 + k % 2)));
            }
        }
        var random = new Random(52);
        assertTimeoutPreemptively(Duration.ofSeconds(6), () -> {
            for (int query = 0; query < 2_000; query++) {
                long from = random.nextInt(10_000_000);
                var agenda = s.upcoming(from, 50);
                assertEquals(50, agenda.size());
                for (int i = 0; i < agenda.size(); i++) {
                    assertTrue(agenda.get(i).start() >= from);
                    if (i > 0) {
                        var a = agenda.get(i - 1);
                        var b = agenda.get(i);
                        assertTrue(a.start() < b.start() || a.start() == b.start() && a.room().compareTo(b.room()) < 0,
                                "out of order: " + a + " then " + b);
                    }
                }
                var slot = s.firstFreeEverywhere(names.subList(0, 50), from, from + 1_000, 3);
                assertTrue(slot.isPresent());
            }
        }, "2,000 agendas of 2,000 never-ending series: are occurrences being listed far ahead?");
    }
}
