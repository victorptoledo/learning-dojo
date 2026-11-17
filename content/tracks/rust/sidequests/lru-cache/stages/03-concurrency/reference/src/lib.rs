//! A thread-safe LRU cache with hit/miss/eviction counters.
//!
//! Entries live in a slab (`Vec<Option<Entry>>`) linked from most to least recently used;
//! a `HashMap` maps each key to its slot, so `get` and `put` are O(1). One `Mutex` guards
//! the whole state.

use std::collections::HashMap;
use std::fmt::Write;
use std::hash::Hash;
use std::sync::{Mutex, MutexGuard};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct Stats {
    pub hits: u64,
    pub misses: u64,
    pub evictions: u64,
}

impl Stats {
    pub fn hit_ratio(&self) -> f64 {
        let lookups = self.hits + self.misses;
        if lookups == 0 {
            0.0
        } else {
            self.hits as f64 / lookups as f64
        }
    }
}

struct Entry<K, V> {
    key: K,
    value: V,
    newer: Option<usize>,
    older: Option<usize>,
}

struct Inner<K, V> {
    slots: Vec<Option<Entry<K, V>>>,
    free: Vec<usize>,
    index: HashMap<K, usize>,
    newest: Option<usize>,
    oldest: Option<usize>,
    stats: Stats,
}

impl<K: Eq + Hash + Clone, V: Clone> Inner<K, V> {
    fn entry(&mut self, slot: usize) -> &mut Entry<K, V> {
        self.slots[slot].as_mut().expect("linked slots are filled")
    }

    fn unlink(&mut self, slot: usize) {
        let (newer, older) = {
            let entry = self.entry(slot);
            (entry.newer.take(), entry.older.take())
        };
        match newer {
            Some(newer) => self.entry(newer).older = older,
            None => self.newest = older,
        }
        match older {
            Some(older) => self.entry(older).newer = newer,
            None => self.oldest = newer,
        }
    }

    fn push_newest(&mut self, slot: usize) {
        let previous = self.newest;
        {
            let entry = self.entry(slot);
            entry.newer = None;
            entry.older = previous;
        }
        if let Some(previous) = previous {
            self.entry(previous).newer = Some(slot);
        }
        self.newest = Some(slot);
        if self.oldest.is_none() {
            self.oldest = Some(slot);
        }
    }

    fn lookup(&mut self, key: &K) -> Option<V> {
        let Some(&slot) = self.index.get(key) else {
            self.stats.misses += 1;
            return None;
        };
        self.stats.hits += 1;
        self.unlink(slot);
        self.push_newest(slot);
        Some(self.entry(slot).value.clone())
    }

    fn insert(&mut self, key: K, value: V, capacity: usize) {
        if let Some(&slot) = self.index.get(&key) {
            self.entry(slot).value = value;
            self.unlink(slot);
            self.push_newest(slot);
            return;
        }
        if self.index.len() == capacity
            && let Some(oldest) = self.oldest
        {
            self.unlink(oldest);
            if let Some(evicted) = self.slots[oldest].take() {
                self.index.remove(&evicted.key);
            }
            self.free.push(oldest);
            self.stats.evictions += 1;
        }
        let entry = Entry {
            key: key.clone(),
            value,
            newer: None,
            older: None,
        };
        let slot = match self.free.pop() {
            Some(slot) => {
                self.slots[slot] = Some(entry);
                slot
            }
            None => {
                self.slots.push(Some(entry));
                self.slots.len() - 1
            }
        };
        self.index.insert(key, slot);
        self.push_newest(slot);
    }
}

pub struct Cache<K, V> {
    capacity: usize,
    inner: Mutex<Inner<K, V>>,
}

impl<K: Eq + Hash + Clone, V: Clone> Cache<K, V> {
    pub fn new(capacity: usize) -> Self {
        assert!(capacity > 0, "a cache needs room for at least one entry");
        Self {
            capacity,
            inner: Mutex::new(Inner {
                slots: Vec::with_capacity(capacity),
                free: Vec::new(),
                index: HashMap::with_capacity(capacity),
                newest: None,
                oldest: None,
                stats: Stats::default(),
            }),
        }
    }

    fn lock(&self) -> MutexGuard<'_, Inner<K, V>> {
        // A panic in another thread leaves the state consistent: every method either
        // finishes its update or changes nothing before it can panic.
        self.inner
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner())
    }

    pub fn get(&self, key: &K) -> Option<V> {
        self.lock().lookup(key)
    }

    pub fn put(&self, key: K, value: V) {
        self.lock().insert(key, value, self.capacity);
    }

    /// Holds the lock while `make` runs, so a key is computed at most once.
    pub fn get_or_insert_with(&self, key: K, make: impl FnOnce() -> V) -> V {
        let mut inner = self.lock();
        if let Some(value) = inner.lookup(&key) {
            return value;
        }
        let value = make();
        inner.insert(key, value.clone(), self.capacity);
        value
    }

    pub fn len(&self) -> usize {
        self.lock().index.len()
    }

    pub fn is_empty(&self) -> bool {
        self.len() == 0
    }

    pub fn capacity(&self) -> usize {
        self.capacity
    }

    pub fn stats(&self) -> Stats {
        self.lock().stats
    }

    pub fn render_metrics(&self, prefix: &str) -> String {
        let (stats, entries) = {
            let inner = self.lock();
            (inner.stats, inner.index.len())
        };
        let series = [
            (
                "hits_total",
                "counter",
                "Lookups that found the key.",
                stats.hits,
            ),
            (
                "misses_total",
                "counter",
                "Lookups that did not find the key.",
                stats.misses,
            ),
            (
                "evictions_total",
                "counter",
                "Entries removed to make room.",
                stats.evictions,
            ),
            (
                "entries",
                "gauge",
                "Entries in the cache now.",
                entries as u64,
            ),
            (
                "capacity",
                "gauge",
                "Most entries the cache holds.",
                self.capacity as u64,
            ),
        ];
        let mut text = String::new();
        for (name, kind, help, value) in series {
            let _ = writeln!(text, "# HELP {prefix}_{name} {help}");
            let _ = writeln!(text, "# TYPE {prefix}_{name} {kind}");
            let _ = writeln!(text, "{prefix}_{name} {value}");
        }
        text
    }
}
