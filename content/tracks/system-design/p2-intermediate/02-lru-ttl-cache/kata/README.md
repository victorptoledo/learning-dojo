# LRU + TTL Cache

`LruTtlCache<K, V>` holds at most `capacity` entries. Each entry expires `ttl` after it was
written; the least recently **used** entry (read or written) is evicted when room is needed.
"Now" always comes from the `Clock`.

## `LruTtlCache(int capacity, Duration defaultTtl, Clock clock)`

- A capacity below 1, or a TTL that is zero or negative, throws `IllegalArgumentException`.

| Method | Behaviour |
| --- | --- |
| `put(key, value)` | stores with the default TTL |
| `put(key, value, ttl)` | stores with its own TTL; overwriting a key replaces value and expiry and makes it the most recent |
| `get(key)` | `Optional` of the value; a hit makes the entry the most recent |
| `getOrLoad(key, loader)` | the cached value, or calls `loader` **once**, stores the result and returns it |
| `invalidate(key)` | removes the key; `true` if it was there and not expired |
| `size()` | live entries only (expired ones are removed first) |
| `stats()` | `CacheStats(hits, misses, evictions, expirations)` |

## The rules

- An entry written at `t` with TTL `d` is **expired from `t + d` on** (at exactly `t + d` it is
  gone).
- `get` of an expired entry removes it: one **expiration** and one **miss**.
- `get` and `getOrLoad` count a **hit** or a **miss**. `put` counts neither.
- Inserting a **new** key into a full cache first removes every expired entry (each counts as an
  **expiration**). Only if it is still full is the least recently used entry removed: one
  **eviction**.
- The cache is **thread-safe**, and `getOrLoad` never runs the loader twice for the same missing
  key at the same time: 16 threads asking for one cold key cause one load.

> Tip: `new LinkedHashMap<>(16, 0.75f, true)` keeps entries in **access order**: `get` moves an
> entry to the end, so the first entry of the iterator is the least recently used. Store a small
> record of value and expiry instant. `synchronized` methods are enough here, and holding the
> lock while loading is what makes `getOrLoad` load once (a production cache would lock per key).
