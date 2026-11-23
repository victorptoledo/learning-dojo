package dojo.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class Stage1BookingsTest {

    private static List<String> ids(List<Booking> bookings) {
        return bookings.stream().map(Booking::id).toList();
    }

    @Test
    void theReadmeExample() {
        var s = new Scheduler();
        s.addRoom("orion");
        assertTrue(s.book("standup", "orion", 540, 555));
        assertFalse(s.book("review", "orion", 550, 600));
        assertTrue(s.book("review", "orion", 555, 600));
        assertEquals(Optional.of(new Booking("standup", "orion", 540, 555)), s.at("orion", 554));
        assertEquals(List.of("standup"), ids(s.between("orion", 0, 541)));
        assertEquals(2, s.size());
    }

    @Test
    void halfOpenIntervalsTouchWithoutOverlapping() {
        var s = new Scheduler();
        s.addRoom("a");
        assertTrue(s.book("m2", "a", 100, 200));
        assertTrue(s.book("m1", "a", 50, 100));
        assertTrue(s.book("m3", "a", 200, 201));
        assertFalse(s.book("x", "a", 99, 101));
        assertFalse(s.book("x", "a", 120, 130), "inside another booking");
        assertFalse(s.book("x", "a", 0, 300), "around every booking");
        assertFalse(s.book("x", "a", 199, 200));
        assertEquals(Optional.empty(), s.at("a", 201));
        assertEquals(Optional.empty(), s.at("a", 49));
        assertEquals("m2", s.at("a", 100).orElseThrow().id());
        assertEquals("m1", s.at("a", 99).orElseThrow().id());
        assertEquals(3, s.size(), "refused bookings are not kept");
    }

    @Test
    void roomsAreIndependent() {
        var s = new Scheduler();
        s.addRoom("a");
        s.addRoom("b");
        assertTrue(s.book("m1", "a", 0, 60));
        assertTrue(s.book("m2", "b", 0, 60), "the same time in another room");
        assertEquals(List.of("m2"), ids(s.between("b", 0, 60)));
        assertEquals(Optional.empty(), s.at("a", 60));
    }

    @Test
    void betweenListsEveryBookingThatOverlapsTheWindow() {
        var s = new Scheduler();
        s.addRoom("a");
        s.book("early", "a", 0, 100);
        s.book("mid", "a", 150, 250);
        s.book("late", "a", 300, 400);
        assertEquals(List.of("early", "mid"), ids(s.between("a", 50, 151)));
        assertEquals(List.of("mid"), ids(s.between("a", 100, 150 + 1)));
        assertEquals(List.of(), ids(s.between("a", 100, 150)), "a gap");
        assertEquals(List.of("early", "mid", "late"), ids(s.between("a", -1_000, 1_000)));
        assertEquals(List.of("late"), ids(s.between("a", 399, 400)));
        assertEquals(List.of(), ids(s.between("a", 400, 500)));
        assertEquals(List.of(), ids(s.between("a", 120, 120)));
    }

    @Test
    void aCancelledIdCanBeBookedAgain() {
        var s = new Scheduler();
        s.addRoom("a");
        s.book("m", "a", 10, 20);
        assertTrue(s.cancel("m"));
        assertFalse(s.cancel("m"));
        assertFalse(s.cancel("never"));
        assertEquals(0, s.size());
        assertEquals(Optional.empty(), s.at("a", 15));
        assertTrue(s.book("m", "a", 12, 18));
        assertTrue(s.book("n", "a", 10, 12), "the freed time is free");
    }

    @Test
    void badArgumentsAreRefused() {
        var s = new Scheduler();
        s.addRoom("a");
        s.book("m", "a", 10, 20);
        assertThrows(IllegalArgumentException.class, () -> s.addRoom("a"));
        assertThrows(IllegalArgumentException.class, () -> s.addRoom(" "));
        assertThrows(IllegalArgumentException.class, () -> s.addRoom(null));
        assertThrows(IllegalArgumentException.class, () -> s.book("m", "a", 30, 40), "repeated id");
        assertThrows(IllegalArgumentException.class, () -> s.book(null, "a", 30, 40));
        assertThrows(IllegalArgumentException.class, () -> s.book("", "a", 30, 40));
        assertThrows(IllegalArgumentException.class, () -> s.book("n", "zzz", 30, 40));
        assertThrows(IllegalArgumentException.class, () -> s.book("n", "a", 40, 40));
        assertThrows(IllegalArgumentException.class, () -> s.book("n", "a", 41, 40));
        assertThrows(IllegalArgumentException.class, () -> s.at("zzz", 0));
        assertThrows(IllegalArgumentException.class, () -> s.between("zzz", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> s.between("a", 5, 4));
        assertEquals(1, s.size());
    }

    @Test
    void randomBookingsMatchAListScan() {
        var random = new Random(21);
        var s = new Scheduler();
        s.addRoom("r");
        var kept = new ArrayList<Booking>();
        for (int step = 0; step < 5_000; step++) {
            if (!kept.isEmpty() && random.nextInt(4) == 0) {
                Booking gone = kept.remove(random.nextInt(kept.size()));
                assertTrue(s.cancel(gone.id()));
            } else {
                long start = random.nextInt(10_000);
                long end = start + 1 + random.nextInt(40);
                String id = "b" + step;
                boolean free = kept.stream().noneMatch(b -> b.start() < end && start < b.end());
                assertEquals(free, s.book(id, "r", start, end), "book [" + start + ", " + end + ")");
                if (free) {
                    kept.add(new Booking(id, "r", start, end));
                }
            }
            long time = random.nextInt(10_100);
            Optional<Booking> expected = kept.stream().filter(b -> b.start() <= time && time < b.end()).findFirst();
            assertEquals(expected, s.at("r", time), "at " + time);
            long from = random.nextInt(10_000);
            long to = from + random.nextInt(300);
            List<Booking> window = kept.stream()
                    .filter(b -> b.start() < to && from < b.end())
                    .sorted(Comparator.comparingLong(Booking::start))
                    .toList();
            if (from == to) {
                window = List.of();
            }
            assertEquals(window, s.between("r", from, to), "between " + from + " and " + to);
            assertEquals(kept.size(), s.size());
        }
    }

    @Test
    void aBusyRoomAnswersInLogarithmicTime() {
        var starts = new ArrayList<Long>();
        for (long i = 0; i < 200_000; i++) {
            starts.add(i * 10);
        }
        Collections.shuffle(starts, new Random(22));
        var s = new Scheduler();
        s.addRoom("hall");
        var random = new Random(23);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            for (long start : starts) {
                assertTrue(s.book("m" + start, "hall", start, start + 6));
            }
            for (long start : starts) {
                assertFalse(s.book("x" + start, "hall", start + 5, start + 7));
            }
            int found = 0;
            int listed = 0;
            for (int i = 0; i < 200_000; i++) {
                long time = random.nextInt(2_000_000);
                if (s.at("hall", time).isPresent()) {
                    found++;
                }
                listed += s.between("hall", time, time + 25).size();
            }
            assertTrue(found > 100_000 && found < 140_000, "about 6 minutes in 10 are booked: " + found);
            assertTrue(listed > 500_000, "a window of 25 minutes holds 2 to 3 bookings: " + listed);
            for (int i = 0; i < 100_000; i++) {
                assertTrue(s.cancel("m" + starts.get(i)));
            }
            assertEquals(100_000, s.size());
        }, "200,000 bookings and 200,000 questions: is something scanning the whole room?");
    }
}
