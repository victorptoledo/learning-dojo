package dojo.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class Stage3CommonFreeTimeTest {

    private static Scheduler readmeOffice() {
        var s = new Scheduler();
        s.addRoom("orion");
        s.addRoom("lyra");
        s.book("o1", "orion", 540, 555);
        s.book("o2", "orion", 600, 660);
        s.book("l1", "lyra", 500, 570);
        s.book("l2", "lyra", 640, 700);
        return s;
    }

    @Test
    void theReadmeExample() {
        var s = readmeOffice();
        assertEquals(List.of(new Slot(500, 540), new Slot(555, 600), new Slot(660, 700)), s.freeSlots("orion", 500, 700));
        var both = List.of("orion", "lyra");
        assertEquals(Optional.of(new Slot(570, 600)), s.firstFreeEverywhere(both, 500, 800, 30));
        assertEquals(Optional.of(new Slot(700, 731)), s.firstFreeEverywhere(both, 500, 800, 31));
        assertEquals(Optional.empty(), s.firstFreeEverywhere(both, 500, 720, 31));
    }

    @Test
    void freeSlotsAreClippedToTheWindow() {
        var s = readmeOffice();
        assertEquals(List.of(new Slot(555, 600)), s.freeSlots("orion", 545, 610));
        assertEquals(List.of(), s.freeSlots("orion", 600, 660), "fully booked");
        assertEquals(List.of(new Slot(0, 540), new Slot(555, 600), new Slot(660, 10_000)), s.freeSlots("orion", 0, 10_000));
        assertEquals(List.of(), s.freeSlots("orion", 100, 100));
        var empty = new Scheduler();
        empty.addRoom("x");
        assertEquals(List.of(new Slot(-5, 5)), empty.freeSlots("x", -5, 5));
    }

    @Test
    void theSlotMayStartRightAtFromOrEndRightAtTo() {
        var s = readmeOffice();
        assertEquals(Optional.of(new Slot(570, 600)), s.firstFreeEverywhere(List.of("lyra", "orion"), 570, 600, 30));
        assertEquals(Optional.of(new Slot(555, 565)), s.firstFreeEverywhere(List.of("orion"), 545, 565, 10),
                "from inside a booking: the slot starts when it ends");
        assertEquals(Optional.empty(), s.firstFreeEverywhere(List.of("orion"), 545, 564, 10));
        assertEquals(Optional.of(new Slot(0, 500)), s.firstFreeEverywhere(List.of("orion", "lyra"), 0, 500, 500));
        assertEquals(Optional.of(new Slot(570, 580)), s.firstFreeEverywhere(List.of("lyra", "lyra"), 520, 600, 10),
                "a room listed twice is still one room");
    }

    @Test
    void badSearchesAreRefused() {
        var s = readmeOffice();
        assertThrows(IllegalArgumentException.class, () -> s.freeSlots("orion", 5, 4));
        assertThrows(IllegalArgumentException.class, () -> s.freeSlots("nope", 0, 4));
        assertThrows(IllegalArgumentException.class, () -> s.firstFreeEverywhere(null, 0, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> s.firstFreeEverywhere(List.of(), 0, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> s.firstFreeEverywhere(List.of("orion", "nope"), 0, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> s.firstFreeEverywhere(List.of("orion"), 0, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> s.firstFreeEverywhere(List.of("orion"), 10, 0, 1));
    }

    @Test
    void randomOfficesMatchAMinuteByMinuteScan() {
        var random = new Random(41);
        for (int round = 0; round < 150; round++) {
            var s = new Scheduler();
            int roomCount = 1 + random.nextInt(5);
            var names = new ArrayList<String>();
            boolean[][] busy = new boolean[roomCount][400];
            for (int r = 0; r < roomCount; r++) {
                names.add("r" + r);
                s.addRoom("r" + r);
                for (int b = 0; b < 30; b++) {
                    int start = random.nextInt(380);
                    int end = start + 1 + random.nextInt(20);
                    if (s.book("b" + r + "-" + b, "r" + r, start, end)) {
                        for (int t = start; t < end; t++) {
                            busy[r][t] = true;
                        }
                    }
                }
            }
            for (int query = 0; query < 20; query++) {
                int from = random.nextInt(400);
                int to = from + random.nextInt(400 - from);
                int length = 1 + random.nextInt(15);
                var subset = new ArrayList<String>();
                for (int r = 0; r < roomCount; r++) {
                    if (random.nextBoolean() || subset.isEmpty() && r == roomCount - 1) {
                        subset.add("r" + r);
                    }
                }
                Optional<Slot> expected = Optional.empty();
                for (int t = from; t + length <= to && expected.isEmpty(); t++) {
                    boolean free = true;
                    for (String name : subset) {
                        int r = Integer.parseInt(name.substring(1));
                        for (int m = t; m < t + length; m++) {
                            free &= !busy[r][m];
                        }
                    }
                    if (free) {
                        expected = Optional.of(new Slot(t, t + length));
                    }
                }
                assertEquals(expected, s.firstFreeEverywhere(subset, from, to, length),
                        "rooms " + subset + " in [" + from + ", " + to + ") for " + length);
                var slots = new ArrayList<Slot>();
                for (int t = from; t < to; t++) {
                    if (busy[0][t]) {
                        continue;
                    }
                    int end = t;
                    while (end < to && !busy[0][end]) {
                        end++;
                    }
                    slots.add(new Slot(t, end));
                    t = end;
                }
                assertEquals(slots, s.freeSlots("r0", from, to), "free slots of r0 in [" + from + ", " + to + ")");
            }
        }
    }

    @Test
    void aThousandRoomsAreMergedLazily() {
        var s = new Scheduler();
        var names = new ArrayList<String>();
        for (int r = 0; r < 1_000; r++) {
            String name = "room" + r;
            names.add(name);
            s.addRoom(name);
            // every hour from minute 0 to minute 40..49, depending on the room: all free from 49 to 60
            for (int h = 0; h < 200; h++) {
                long start = h * 60L;
                s.book(name + "-" + h, name, start, start + 40 + r % 10);
            }
        }
        var random = new Random(42);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            for (int query = 0; query < 2_000; query++) {
                long from = random.nextInt(11_000);
                var slot = s.firstFreeEverywhere(names, from, 12_100, 5).orElseThrow();
                assertTrue(slot.start() >= from && slot.start() - from <= 60, "the answer is near from: " + slot + " for " + from);
                assertTrue(slot.start() % 60 >= 49 && slot.start() % 60 <= 55, "inside the free minutes: " + slot);
            }
            var few = names.subList(0, 3);
            for (int query = 0; query < 2_000; query++) {
                long from = random.nextInt(11_000);
                var slot = s.firstFreeEverywhere(few, from, 12_100, 15).orElseThrow();
                assertTrue(slot.start() - from <= 60);
            }
        }, "2,000 searches over 1,000 rooms: are all their bookings collected and sorted each time?");
    }
}
