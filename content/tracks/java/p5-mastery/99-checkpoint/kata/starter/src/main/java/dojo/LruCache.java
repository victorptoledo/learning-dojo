package dojo;

import java.util.function.Function;

/** A bounded cache that evicts the least recently used entry. Not written yet. */
public final class LruCache<K, V> {

    public LruCache(int capacity) {
        // TODO
    }

    /** The cached value, or the loader's value (then cached) on a miss. */
    public V get(K key, Function<? super K, ? extends V> loader) {
        throw new UnsupportedOperationException("write LruCache.get");
    }

    public int size() {
        throw new UnsupportedOperationException("write LruCache.size");
    }

    public long hits() {
        throw new UnsupportedOperationException("write LruCache.hits");
    }

    public long misses() {
        throw new UnsupportedOperationException("write LruCache.misses");
    }
}
