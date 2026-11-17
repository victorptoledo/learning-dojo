Many threads, one cache.

- `Cache<K, V>` is `Send + Sync` whenever `K` and `V` are `Send`, so an `Arc<Cache<K, V>>` can
  go to other threads. Under any mix of calls from many threads the cache never holds more than
  its capacity, and `hits + misses` equals the number of `get` calls exactly.
- A new method computes a missing value **at most once**, even when many threads ask for the
  same key at the same moment:

```rust
impl<K: Eq + Hash + Clone, V: Clone> Cache<K, V> {
    /// The value of `key`; when it is missing, `make()` computes it and it is inserted.
    /// A hit counts as a hit, a miss (with the computation) as one miss.
    pub fn get_or_insert_with(&self, key: K, make: impl FnOnce() -> V) -> V;
}
```

Think about what happens between "not found" and "inserted" when two threads run that line
at once. Holding the lock while `make` runs is a correct start; can you do better for
different keys?
