package dojo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** A bounded cache that evicts the least recently used entry. Safe to share between threads. */
public final class LruCache<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, V> entries;
    private long hits;
    private long misses;

    public LruCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        this.capacity = capacity;
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LruCache.this.capacity;
            }
        };
    }

    /**
     * The cached value, or the loader's value (then cached) on a miss. The loader runs outside
     * the lock, so a slow lookup does not block the other threads.
     */
    public V get(K key, Function<? super K, ? extends V> loader) {
        synchronized (this) {
            V cached = entries.get(key);
            if (cached != null) {
                hits++;
                return cached;
            }
            misses++;
        }
        V loaded = loader.apply(key);
        synchronized (this) {
            entries.put(key, loaded);
        }
        return loaded;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized long hits() {
        return hits;
    }

    public synchronized long misses() {
        return misses;
    }
}
