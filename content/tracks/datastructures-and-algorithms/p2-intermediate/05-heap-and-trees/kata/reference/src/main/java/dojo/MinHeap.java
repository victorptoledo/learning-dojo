package dojo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/** A binary min-heap stored in an array. */
public final class MinHeap<T> {
    private final List<T> items;
    private final Comparator<? super T> order;

    public MinHeap(Comparator<? super T> order) {
        this(new ArrayList<>(), order);
    }

    private MinHeap(List<T> items, Comparator<? super T> order) {
        this.items = items;
        this.order = order;
    }

    public static <T> MinHeap<T> heapify(Collection<? extends T> items, Comparator<? super T> order) {
        var heap = new MinHeap<T>(new ArrayList<>(items), order);
        for (int i = heap.items.size() / 2 - 1; i >= 0; i--) {
            heap.siftDown(i);
        }
        return heap;
    }

    public void add(T item) {
        items.add(item);
        siftUp(items.size() - 1);
    }

    public T peek() {
        if (items.isEmpty()) throw new NoSuchElementException("the heap is empty");
        return items.getFirst();
    }

    public T poll() {
        T top = peek();
        T last = items.removeLast();
        if (!items.isEmpty()) {
            items.set(0, last);
            siftDown(0);
        }
        return top;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    private void siftUp(int i) {
        T item = items.get(i);
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (order.compare(item, items.get(parent)) >= 0) break;
            items.set(i, items.get(parent));
            i = parent;
        }
        items.set(i, item);
    }

    private void siftDown(int i) {
        int n = items.size();
        T item = items.get(i);
        while (true) {
            int child = 2 * i + 1;
            if (child >= n) break;
            if (child + 1 < n && order.compare(items.get(child + 1), items.get(child)) < 0) child++;
            if (order.compare(items.get(child), item) >= 0) break;
            items.set(i, items.get(child));
            i = child;
        }
        items.set(i, item);
    }
}
