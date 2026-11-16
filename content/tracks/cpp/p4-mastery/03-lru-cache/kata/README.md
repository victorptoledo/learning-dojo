# LRU Cache

Write `LruCache<K, V, Hash>` in `lru.hpp` (a template, so it lives in the header).

| Member | Does |
| --- | --- |
| `LruCache(capacity, on_evict = {})` | `std::invalid_argument` when `capacity` is 0. |
| `get(key)` | A pointer to the value, which becomes the most recently used entry, or `nullptr`. Counts a hit or a miss. |
| `peek(key) const` | A pointer to the value without changing recency or the counters. |
| `put(key, value)` | Inserts or replaces; the entry becomes the most recent. A new key that does not fit first evicts the least recently used entry and calls `on_evict(key, value)` with it. Replacing evicts nothing. |
| `erase(key)` | Removes the entry without calling `on_evict`; `false` if it was not there. |
| `keys() const` | The keys, most recently used first. |
| `size`, `capacity`, `hits`, `misses` | Already written. |

```cpp
dojo::LruCache<std::string, int> cache(2);
cache.put("a", 1);
cache.put("b", 2);
cache.get("a");     // "a" is now the most recent
cache.put("c", 3);  // evicts "b"
```

It must work with a custom hash (`LruCache<Point, V, PointHash>`) and with move-only values
(`std::unique_ptr`): move values, never copy them.

**Every operation must be O(1) on average.** A hidden test runs 400,000 operations on a
cache of 50,000 entries and fails after 4 seconds; searching the list for a key would take
minutes. The members are a hint: a `std::list` keeps the recency order and
`std::list::splice` moves a node to the front without invalidating iterators, and a
`std::unordered_map` finds a key's node.

10 hidden tests and the visible one, under AddressSanitizer, plus the tool checks.
