package dojo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** A bounded, thread-safe cache with LRU eviction and a time to live per entry. */
public final class LruTtlCache<K, V> {
    private record Entry<V>(V value, Instant expiresAt) {}

    private final int capacity;
    private final Duration defaultTtl;
    private final Clock clock;
    private final LinkedHashMap<K, Entry<V>> entries = new LinkedHashMap<>(16, 0.75f, true);
    private long hits;
    private long misses;
    private long evictions;
    private long expirations;

    public LruTtlCache(int capacity, Duration defaultTtl, Clock clock) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        requirePositive(defaultTtl);
        this.capacity = capacity;
        this.defaultTtl = defaultTtl;
        this.clock = clock;
    }

    public void put(K key, V value) {
        put(key, value, defaultTtl);
    }

    public synchronized void put(K key, V value, Duration ttl) {
        requirePositive(ttl);
        Instant now = clock.instant();
        if (!entries.containsKey(key) && entries.size() >= capacity) {
            makeRoom(now);
        }
        entries.put(key, new Entry<>(value, now.plus(ttl)));
    }

    public synchronized Optional<V> get(K key) {
        Optional<V> value = lookup(key, clock.instant());
        if (value.isPresent()) {
            hits++;
        } else {
            misses++;
        }
        return value;
    }

    /** The cached value, or the loader's result (loaded once, then cached). */
    public synchronized V getOrLoad(K key, Function<? super K, ? extends V> loader) {
        Optional<V> cached = lookup(key, clock.instant());
        if (cached.isPresent()) {
            hits++;
            return cached.get();
        }
        misses++;
        V loaded = loader.apply(key);
        put(key, loaded);
        return loaded;
    }

    /** Removes the key; true if a live entry was there. */
    public synchronized boolean invalidate(K key) {
        Entry<V> removed = entries.remove(key);
        return removed != null && clock.instant().isBefore(removed.expiresAt());
    }

    /** Live entries (expired ones are removed first). */
    public synchronized int size() {
        removeExpired(clock.instant());
        return entries.size();
    }

    public synchronized CacheStats stats() {
        return new CacheStats(hits, misses, evictions, expirations);
    }

    private Optional<V> lookup(K key, Instant now) {
        Entry<V> entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (!now.isBefore(entry.expiresAt())) {
            entries.remove(key);
            expirations++;
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    private void makeRoom(Instant now) {
        removeExpired(now);
        if (entries.size() >= capacity) {
            Iterator<K> eldest = entries.keySet().iterator();
            eldest.next();
            eldest.remove();
            evictions++;
        }
    }

    private void removeExpired(Instant now) {
        Iterator<Map.Entry<K, Entry<V>>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            if (!now.isBefore(it.next().getValue().expiresAt())) {
                it.remove();
                expirations++;
            }
        }
    }

    private static void requirePositive(Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("a TTL must be positive");
        }
    }
}
