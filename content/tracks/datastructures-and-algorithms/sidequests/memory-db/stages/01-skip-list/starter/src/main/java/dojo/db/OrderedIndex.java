package dojo.db;

import java.util.Comparator;

/** A sorted map from keys to values, written by hand. See README.md. */
public final class OrderedIndex<K, V> {

    public OrderedIndex(Comparator<? super K> order) {
        // todo
    }

    public V put(K key, V value) {
        throw new UnsupportedOperationException("todo");
    }

    public V get(K key) {
        throw new UnsupportedOperationException("todo");
    }

    public V remove(K key) {
        throw new UnsupportedOperationException("todo");
    }

    public int size() {
        throw new UnsupportedOperationException("todo");
    }

    public K first() {
        throw new UnsupportedOperationException("todo");
    }

    public K last() {
        throw new UnsupportedOperationException("todo");
    }

    public K floor(K key) {
        throw new UnsupportedOperationException("todo");
    }

    public K ceiling(K key) {
        throw new UnsupportedOperationException("todo");
    }
}
