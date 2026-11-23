Redirects are read a hundred times more than links are written, and every `store.get` is a
round trip. Put a cache in front of the store.

- `resolve` answers from the cache when it can, without calling the store. On a miss it
  reads the store and caches the answer: a URL that was found for `config.cacheTtl()`, and
  an absent code (a **negative entry**) for `config.negativeTtl()`. A scanner trying random
  codes must not reach the database twice for the same code.
- The cache holds at most `config.cacheCapacity()` entries, negative ones included. When it
  is full, the least recently used entry goes (a hit or a new entry counts as a use).
- An entry is good until its time ends: with a TTL of 10 minutes, a link cached at 12:00 is
  served from the cache at 12:09:59 and read from the store again at 12:10.
- A link this shortener creates goes into the cache at once (write-through), replacing a
  negative entry for the same code: an alias created right after somebody looked it up
  resolves at once.

New in the starter, `CacheStats(long hits, long misses, long evictions)`, and two methods:

```java
public boolean delete(String code)   // store.delete(code), and the code leaves this cache
public CacheStats stats()
```

- A hit is a `resolve` answered by the cache, negative entries included; a miss is one that
  read the store (an expired entry is a miss). An eviction is an entry pushed out because
  the cache was full; expired entries do not count.
- `delete` is immediate on the shortener that runs it. Another shortener with the same
  store keeps serving the code from its cache until the entry's time ends: that is the
  staleness a TTL buys, and the tests check it is bounded by the TTL.
