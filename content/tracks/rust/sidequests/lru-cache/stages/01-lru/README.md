In `src/lib.rs`, a public generic type with this API:

```rust
pub struct Cache<K, V> { /* yours */ }

impl<K: Eq + Hash + Clone, V: Clone> Cache<K, V> {
    /// Panics when `capacity` is 0.
    pub fn new(capacity: usize) -> Self;
    /// A clone of the value, and the key becomes the most recently used.
    pub fn get(&self, key: &K) -> Option<V>;
    /// Inserts or replaces; either way the key becomes the most recently used. When a new
    /// key does not fit, the least recently used entry is removed first.
    pub fn put(&self, key: K, value: V);
    pub fn len(&self) -> usize;
    pub fn is_empty(&self) -> bool;
    pub fn capacity(&self) -> usize;
}
```

"Used" means read with `get` or written with `put`. Replacing the value of a key that is
already there never evicts anything.
