package dojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Problems solved with your MinHeap. */
public final class Heaps {
    private Heaps() {}

    public static List<Integer> largest(int[] values, int k) {
        if (k <= 0) return List.of();
        var heap = new MinHeap<Integer>(Comparator.naturalOrder());
        for (int value : values) {
            if (heap.size() < k) {
                heap.add(value);
            } else if (value > heap.peek()) {
                heap.poll();
                heap.add(value);
            }
        }
        var out = new ArrayList<Integer>(heap.size());
        while (!heap.isEmpty()) out.add(heap.poll());
        Collections.reverse(out);
        return out;
    }

    private record Cursor(int[] array, int index) {
        int value() {
            return array[index];
        }
    }

    public static int[] mergeSorted(List<int[]> arrays) {
        int total = 0;
        var heap = new MinHeap<Cursor>(Comparator.comparingInt(Cursor::value));
        for (int[] array : arrays) {
            total += array.length;
            if (array.length > 0) heap.add(new Cursor(array, 0));
        }
        int[] out = new int[total];
        int next = 0;
        while (!heap.isEmpty()) {
            Cursor c = heap.poll();
            out[next++] = c.value();
            if (c.index() + 1 < c.array().length) heap.add(new Cursor(c.array(), c.index() + 1));
        }
        return out;
    }
}
