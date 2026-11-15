package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

class DynamicArrayGradingTest {

    private static List<Integer> contents(DynamicArray<Integer> list) {
        var out = new ArrayList<Integer>();
        for (Integer value : list) {
            out.add(value);
        }
        return out;
    }

    @Test
    void random_operations_match_a_reference_list() {
        var random = new Random(7);
        var expected = new ArrayList<Integer>();
        var actual = new DynamicArray<Integer>();
        for (int step = 0; step < 20_000; step++) {
            int op = random.nextInt(6);
            if (op <= 1 || expected.isEmpty()) {
                int v = random.nextInt(1000);
                expected.add(v);
                actual.add(v);
            } else if (op == 2) {
                int i = random.nextInt(expected.size() + 1);
                expected.add(i, step);
                actual.add(i, step);
            } else if (op == 3) {
                int i = random.nextInt(expected.size());
                assertEquals(expected.remove(i), actual.remove(i));
            } else if (op == 4) {
                assertEquals(expected.removeLast(), actual.removeLast());
            } else {
                int i = random.nextInt(expected.size());
                assertEquals(expected.set(i, -step), actual.set(i, -step));
            }
            assertEquals(expected.size(), actual.size());
        }
        assertEquals(expected, contents(actual));
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i));
        }
    }

    @Test
    void bad_indexes_and_empty_removal_throw() {
        var list = new DynamicArray<String>();
        assertTrue(list.isEmpty());
        assertThrows(NoSuchElementException.class, list::removeLast);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, "x"));
        list.add(0, "a");
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(1, "b"));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, "b"));
        list.add(null);
        assertNull(list.get(1));
        assertEquals(2, list.size());
    }

    @Test
    void capacity_grows_geometrically() {
        var list = new DynamicArray<Integer>();
        int changes = 0;
        int last = list.capacity();
        assertEquals(4, last);
        for (int i = 0; i < 1_000_000; i++) {
            list.add(i);
            int now = list.capacity();
            if (now != last) {
                changes++;
                last = now;
            }
            assertTrue(now >= list.size());
        }
        assertTrue(changes <= 20, "capacity changed " + changes + " times");
        assertEquals(1 << 20, list.capacity(), "4, doubled until a million fit");
    }

    @Test
    void shrinks_exactly_at_a_quarter_and_never_below_four() {
        var list = new DynamicArray<Integer>();
        for (int i = 0; i < 64; i++) {
            list.add(i);
        }
        assertEquals(64, list.capacity());
        while (list.size() > 17) {
            list.removeLast();
        }
        assertEquals(64, list.capacity(), "17 of 64 is above a quarter");
        list.removeLast();
        assertEquals(32, list.capacity(), "16 of 64 is a quarter");
        while (!list.isEmpty()) {
            list.remove(0);
        }
        assertEquals(4, list.capacity());
    }

    @Test
    void add_and_remove_at_a_boundary_does_not_thrash() {
        var list = new DynamicArray<Integer>();
        for (int i = 0; i < 1024; i++) {
            list.add(i);
        }
        int changes = 0;
        int last = list.capacity();
        for (int i = 0; i < 100_000; i++) {
            list.add(i);
            list.removeLast();
            if (list.capacity() != last) {
                changes++;
                last = list.capacity();
            }
        }
        assertTrue(changes <= 2, "capacity changed " + changes + " times");
    }

    @Test
    void millions_of_appends_and_removals_are_fast() {
        assertTimeoutPreemptively(Duration.ofSeconds(4), () -> {
            var list = new DynamicArray<Integer>();
            Integer boxed = 1;
            for (int i = 0; i < 3_000_000; i++) {
                list.add(boxed);
            }
            for (int i = 0; i < 3_000_000; i++) {
                list.removeLast();
            }
            assertTrue(list.isEmpty());
        });
    }
}
