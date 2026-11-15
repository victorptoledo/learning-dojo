package dojo;

import java.util.List;

/** A hash map with separate chaining, power-of-two buckets and a load factor of 0.75. */
public final class ChainedHashMap<K, V> {

    public ChainedHashMap() {
        // todo: 16 empty buckets
    }

    /** Stores the value and returns the previous one, or null. */
    public V put(K key, V value) {
        throw new UnsupportedOperationException("todo put " + key);
    }

    /** The value of the key, or null when it is missing. */
    public V get(K key) {
        throw new UnsupportedOperationException("todo get " + key);
    }

    public boolean containsKey(K key) {
        throw new UnsupportedOperationException("todo containsKey " + key);
    }

    /** Removes the key and returns its value, or null. */
    public V remove(K key) {
        throw new UnsupportedOperationException("todo remove " + key);
    }

    public int size() {
        throw new UnsupportedOperationException("todo size");
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException("todo isEmpty");
    }

    /** The length of the bucket array: 16 at first, always a power of two. */
    public int bucketCount() {
        throw new UnsupportedOperationException("todo bucketCount");
    }

    /** The number of entries in the longest chain. */
    public int longestChain() {
        throw new UnsupportedOperationException("todo longestChain");
    }

    /** All keys, in any order. */
    public List<K> keys() {
        throw new UnsupportedOperationException("todo keys");
    }
}
