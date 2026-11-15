package dojo;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

/** A bounded, thread-safe cache with LRU eviction and a time to live per entry. */
public final class LruTtlCache<K, V> {

    public LruTtlCache(int capacity, Duration defaultTtl, Clock clock) {
        throw new UnsupportedOperationException("todo cache of " + capacity);
    }

    public void put(K key, V value) {
        throw new UnsupportedOperationException("todo put " + key);
    }

    public void put(K key, V value, Duration ttl) {
        throw new UnsupportedOperationException("todo put " + key + " for " + ttl);
    }

    public Optional<V> get(K key) {
        throw new UnsupportedOperationException("todo get " + key);
    }

    /** The cached value, or the loader's result (loaded once, then cached). */
    public V getOrLoad(K key, Function<? super K, ? extends V> loader) {
        throw new UnsupportedOperationException("todo get or load " + key);
    }

    /** Removes the key; true if a live entry was there. */
    public boolean invalidate(K key) {
        throw new UnsupportedOperationException("todo invalidate " + key);
    }

    /** Live entries (expired ones are removed first). */
    public int size() {
        throw new UnsupportedOperationException("todo size");
    }

    public CacheStats stats() {
        throw new UnsupportedOperationException("todo stats");
    }
}
