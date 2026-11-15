package dojo;

import java.util.Iterator;

/** A growable array backed by an Object[]: doubles when full, halves at a quarter full. */
public final class DynamicArray<T> implements Iterable<T> {

    public DynamicArray() {
        // todo: allocate the backing array with capacity 4
    }

    /** Appends the value, doubling the capacity first if the array is full. */
    public void add(T value) {
        throw new UnsupportedOperationException("todo add " + value);
    }

    /** Inserts the value at index (0..size), shifting later elements right. */
    public void add(int index, T value) {
        throw new UnsupportedOperationException("todo add at " + index);
    }

    /** The element at index (0..size-1). */
    public T get(int index) {
        throw new UnsupportedOperationException("todo get " + index);
    }

    /** Replaces the element at index and returns the old one. */
    public T set(int index, T value) {
        throw new UnsupportedOperationException("todo set " + index);
    }

    /** Removes and returns the last element; throws NoSuchElementException when empty. */
    public T removeLast() {
        throw new UnsupportedOperationException("todo removeLast");
    }

    /** Removes and returns the element at index, shifting later elements left. */
    public T remove(int index) {
        throw new UnsupportedOperationException("todo remove " + index);
    }

    public int size() {
        throw new UnsupportedOperationException("todo size");
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException("todo isEmpty");
    }

    /** The length of the backing array. */
    public int capacity() {
        throw new UnsupportedOperationException("todo capacity");
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("todo iterator");
    }
}
