package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;
import org.junit.jupiter.api.Test;

class EventWindowGradingTest {
    private record Event(long timestamp, String user, int latency) {}

    /** Scans everything: slow, but obviously right. */
    private static final class SlowWindow {
        final long window;
        final List<Event> all = new ArrayList<>();
        long now;

        SlowWindow(long window) {
            this.window = window;
        }

        List<Event> live() {
            return all.stream().filter(e -> e.timestamp() > now - window).toList();
        }
    }

    private static void assertSame(SlowWindow slow, EventWindow fast) {
        var live = slow.live();
        assertEquals(slow.now, fast.now());
        assertEquals(live.size(), fast.count(), "count at " + slow.now);
        assertEquals(new HashSet<>(live.stream().map(Event::user).toList()).size(), fast.distinctUsers(), "distinct users at " + slow.now);
        for (String user : List.of("u0", "u1", "u2", "u3", "nobody")) {
            assertEquals(live.stream().filter(e -> e.user().equals(user)).count(), fast.countFor(user), user + " at " + slow.now);
        }
        var max = live.stream().mapToInt(Event::latency).max();
        assertEquals(max, fast.maxLatency(), "max latency at " + slow.now);
        var average = live.stream().mapToInt(Event::latency).average();
        assertEquals(average.isPresent(), fast.averageLatency().isPresent());
        if (average.isPresent()) {
            assertEquals(average.getAsDouble(), fast.averageLatency().getAsDouble(), 1e-9);
        }
    }

    @Test
    void random_streams_match_a_slow_window() {
        var random = new Random(21);
        for (int round = 0; round < 40; round++) {
            long windowMillis = 1 + random.nextInt(50);
            var slow = new SlowWindow(windowMillis);
            var fast = new EventWindow(windowMillis);
            for (int step = 0; step < 300; step++) {
                long t = slow.now + random.nextInt(8);
                if (random.nextInt(5) == 0) {
                    slow.now = t;
                    fast.advanceTo(t);
                } else {
                    var e = new Event(t, "u" + random.nextInt(4), random.nextInt(6));
                    slow.now = t;
                    slow.all.add(e);
                    fast.record(e.timestamp(), e.user(), e.latency());
                }
                assertSame(slow, fast);
            }
        }
    }

    @Test
    void the_boundary_is_exclusive() {
        var window = new EventWindow(10);
        window.record(5, "a", 1);
        window.advanceTo(14);
        assertEquals(1, window.count(), "5 > 14 - 10");
        window.advanceTo(15);
        assertEquals(0, window.count(), "5 <= 15 - 10");
        assertEquals(OptionalInt.empty(), window.maxLatency());
        assertEquals(OptionalDouble.empty(), window.averageLatency());
        assertEquals(0, window.distinctUsers());
    }

    @Test
    void equal_latencies_leave_one_at_a_time() {
        var window = new EventWindow(3);
        window.record(1, "a", 7);
        window.record(2, "b", 7);
        window.record(3, "c", 2);
        window.advanceTo(4);
        assertEquals(OptionalInt.of(7), window.maxLatency(), "the second 7 is still there");
        window.advanceTo(5);
        assertEquals(OptionalInt.of(2), window.maxLatency());
    }

    @Test
    void bad_input_is_refused() {
        assertThrows(IllegalArgumentException.class, () -> new EventWindow(0));
        var window = new EventWindow(100);
        window.record(50, "a", 1);
        assertThrows(IllegalArgumentException.class, () -> window.record(49, "a", 1));
        assertThrows(IllegalArgumentException.class, () -> window.advanceTo(10));
        assertThrows(IllegalArgumentException.class, () -> window.record(60, " ", 1));
        assertThrows(IllegalArgumentException.class, () -> window.record(60, null, 1));
        assertThrows(IllegalArgumentException.class, () -> window.record(60, "a", -1));
        window.record(50, "b", 3);
        assertEquals(2, window.count(), "the same timestamp is fine");
    }

    @Test
    void a_million_events_with_a_large_window_are_fast() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            var window = new EventWindow(200_000);
            long checksum = 0;
            for (int t = 1; t <= 1_000_000; t++) {
                int latency = t % 1000 == 0 ? 5_000 : (int) ((t * 7919L) % 1000);
                window.record(t, "user" + (t % 5_000), latency);
                checksum += window.count() + window.distinctUsers() + window.maxLatency().getAsInt();
                checksum += (long) window.averageLatency().getAsDouble();
            }
            assertEquals(200_000, window.count());
            assertEquals(5_000, window.distinctUsers());
            assertEquals(5_000, window.maxLatency().getAsInt());
            assertTrue(checksum > 0);
        });
    }
}
