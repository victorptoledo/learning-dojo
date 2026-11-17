//! Stage 1: an LRU cache behind a `Mutex`, most recent entries at the back.

use std::collections::VecDeque;
use std::sync::Mutex;

pub struct Cache<K, V> {
    capacity: usize,
    entries: Mutex<VecDeque<(K, V)>>,
}

impl<K: Eq + std::hash::Hash + Clone, V: Clone> Cache<K, V> {
    pub fn new(capacity: usize) -> Self {
        assert!(capacity > 0, "a cache needs room for at least one entry");
        Self {
            capacity,
            entries: Mutex::new(VecDeque::new()),
        }
    }

    pub fn get(&self, key: &K) -> Option<V> {
        let mut entries = self.entries.lock().unwrap();
        let position = entries.iter().position(|(k, _)| k == key)?;
        let entry = entries.remove(position)?;
        let value = entry.1.clone();
        entries.push_back(entry);
        Some(value)
    }

    pub fn put(&self, key: K, value: V) {
        let mut entries = self.entries.lock().unwrap();
        if let Some(position) = entries.iter().position(|(k, _)| *k == key) {
            entries.remove(position);
        } else if entries.len() == self.capacity {
            entries.pop_front();
        }
        entries.push_back((key, value));
    }

    pub fn len(&self) -> usize {
        self.entries.lock().unwrap().len()
    }

    pub fn is_empty(&self) -> bool {
        self.len() == 0
    }

    pub fn capacity(&self) -> usize {
        self.capacity
    }
}
