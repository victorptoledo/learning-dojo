package dojo;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** A growable array backed by an Object[]: doubles when full, halves at a quarter full. */
public final class DynamicArray<T> implements Iterable<T> {
    private static final int MIN_CAPACITY = 4;

    private Object[] elements = new Object[MIN_CAPACITY];
    private int size;

    public DynamicArray() {}

    public void add(T value) {
        if (size == elements.length) resize(elements.length * 2);
        elements[size++] = value;
    }

    public void add(int index, T value) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException(index);
        if (size == elements.length) resize(elements.length * 2);
        System.arraycopy(elements, index, elements, index + 1, size - index);
        elements[index] = value;
        size++;
    }

    public T get(int index) {
        checkIndex(index);
        return elementAt(index);
    }

    public T set(int index, T value) {
        checkIndex(index);
        T old = elementAt(index);
        elements[index] = value;
        return old;
    }

    public T removeLast() {
        if (size == 0) throw new NoSuchElementException("the array is empty");
        T old = elementAt(size - 1);
        elements[--size] = null;
        shrinkIfSparse();
        return old;
    }

    public T remove(int index) {
        checkIndex(index);
        T old = elementAt(index);
        System.arraycopy(elements, index + 1, elements, index, size - index - 1);
        elements[--size] = null;
        shrinkIfSparse();
        return old;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return elements.length;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int next;

            @Override
            public boolean hasNext() {
                return next < size;
            }

            @Override
            public T next() {
                if (next >= size) throw new NoSuchElementException();
                return elementAt(next++);
            }
        };
    }

    private void shrinkIfSparse() {
        if (elements.length > MIN_CAPACITY && size <= elements.length / 4) {
            resize(elements.length / 2);
        }
    }

    private void resize(int newCapacity) {
        elements = Arrays.copyOf(elements, newCapacity);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
    }

    @SuppressWarnings("unchecked")
    private T elementAt(int index) {
        return (T) elements[index];
    }
}
