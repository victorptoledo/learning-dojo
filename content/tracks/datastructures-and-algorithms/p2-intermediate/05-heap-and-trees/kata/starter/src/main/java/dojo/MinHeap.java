package dojo;

import java.util.Collection;
import java.util.Comparator;

/** A binary min-heap stored in an array. */
public final class MinHeap<T> {

    public MinHeap(Comparator<? super T> order) {
        throw new UnsupportedOperationException("todo heap");
    }

    /** A heap of all the items, built bottom-up in O(n). */
    public static <T> MinHeap<T> heapify(Collection<? extends T> items, Comparator<? super T> order) {
        throw new UnsupportedOperationException("todo heapify " + items.size() + " items");
    }

    public void add(T item) {
        throw new UnsupportedOperationException("todo add");
    }

    /** The smallest item; NoSuchElementException when empty. */
    public T peek() {
        throw new UnsupportedOperationException("todo peek");
    }

    /** Removes and returns the smallest item; NoSuchElementException when empty. */
    public T poll() {
        throw new UnsupportedOperationException("todo poll");
    }

    public int size() {
        throw new UnsupportedOperationException("todo size");
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException("todo isEmpty");
    }
}
