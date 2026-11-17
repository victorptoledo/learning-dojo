Count how well the cache works, and expose the counts the way Prometheus reads them.

```rust
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct Stats {
    pub hits: u64,      // `get` found the key
    pub misses: u64,    // `get` did not find it
    pub evictions: u64, // entries removed to make room
}

impl Stats {
    /// hits / (hits + misses), or 0.0 before any lookup.
    pub fn hit_ratio(&self) -> f64;
}

impl<K, V> Cache<K, V> {
    pub fn stats(&self) -> Stats;
    /// The metrics in the Prometheus text format, every name starting with `prefix`.
    pub fn render_metrics(&self, prefix: &str) -> String;
}
```

`put` is not a lookup: it changes no hit or miss. `render_metrics("cache")` returns five
metrics, each with its `# HELP` and `# TYPE` lines (the HELP text is yours):

```text
# HELP cache_hits_total Lookups that found the key.
# TYPE cache_hits_total counter
cache_hits_total 3
# HELP cache_misses_total Lookups that did not find the key.
# TYPE cache_misses_total counter
cache_misses_total 1
# HELP cache_evictions_total Entries removed to make room.
# TYPE cache_evictions_total counter
cache_evictions_total 0
# HELP cache_entries Entries in the cache now.
# TYPE cache_entries gauge
cache_entries 2
# HELP cache_capacity Most entries the cache holds.
# TYPE cache_capacity gauge
cache_capacity 2
```

Counters only go up (they end in `_total`); gauges are the current value.
