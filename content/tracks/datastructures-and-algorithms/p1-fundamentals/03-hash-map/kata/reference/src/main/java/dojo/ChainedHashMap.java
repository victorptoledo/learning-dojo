package dojo;

import java.util.ArrayList;
import java.util.List;

/** A hash map with separate chaining, power-of-two buckets and a load factor of 0.75. */
public final class ChainedHashMap<K, V> {
    private static final int INITIAL_BUCKETS = 16;

    private static final class Entry<K, V> {
        final K key;
        V value;
        Entry<K, V> next;

        Entry(K key, V value, Entry<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private Entry<K, V>[] buckets = newBuckets(INITIAL_BUCKETS);
    private int size;

    public ChainedHashMap() {}

    public V put(K key, V value) {
        requireKey(key);
        int index = indexFor(key, buckets.length);
        for (Entry<K, V> e = buckets[index]; e != null; e = e.next) {
            if (e.key.equals(key)) {
                V old = e.value;
                e.value = value;
                return old;
            }
        }
        buckets[index] = new Entry<>(key, value, buckets[index]);
        size++;
        if (size > buckets.length * 3L / 4) resize();
        return null;
    }

    public V get(K key) {
        Entry<K, V> e = find(key);
        return e == null ? null : e.value;
    }

    public boolean containsKey(K key) {
        return find(key) != null;
    }

    public V remove(K key) {
        requireKey(key);
        int index = indexFor(key, buckets.length);
        Entry<K, V> previous = null;
        for (Entry<K, V> e = buckets[index]; e != null; previous = e, e = e.next) {
            if (e.key.equals(key)) {
                if (previous == null) {
                    buckets[index] = e.next;
                } else {
                    previous.next = e.next;
                }
                size--;
                return e.value;
            }
        }
        return null;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int bucketCount() {
        return buckets.length;
    }

    public int longestChain() {
        int longest = 0;
        for (Entry<K, V> head : buckets) {
            int length = 0;
            for (Entry<K, V> e = head; e != null; e = e.next) {
                length++;
            }
            longest = Math.max(longest, length);
        }
        return longest;
    }

    public List<K> keys() {
        List<K> keys = new ArrayList<>(size);
        for (Entry<K, V> head : buckets) {
            for (Entry<K, V> e = head; e != null; e = e.next) {
                keys.add(e.key);
            }
        }
        return keys;
    }

    private Entry<K, V> find(K key) {
        requireKey(key);
        for (Entry<K, V> e = buckets[indexFor(key, buckets.length)]; e != null; e = e.next) {
            if (e.key.equals(key)) return e;
        }
        return null;
    }

    private void resize() {
        Entry<K, V>[] old = buckets;
        buckets = newBuckets(old.length * 2);
        for (Entry<K, V> head : old) {
            Entry<K, V> e = head;
            while (e != null) {
                Entry<K, V> next = e.next;
                int index = indexFor(e.key, buckets.length);
                e.next = buckets[index];
                buckets[index] = e;
                e = next;
            }
        }
    }

    private static int indexFor(Object key, int bucketCount) {
        int h = key.hashCode();
        return (h ^ (h >>> 16)) & (bucketCount - 1);
    }

    private static void requireKey(Object key) {
        if (key == null) throw new IllegalArgumentException("null keys are not allowed");
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Entry<K, V>[] newBuckets(int count) {
        return (Entry<K, V>[]) new Entry[count];
    }
}
