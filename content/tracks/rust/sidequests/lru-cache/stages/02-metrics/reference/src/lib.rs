//! Stage 2: the stage 1 cache plus counters and the Prometheus text format.

use std::collections::VecDeque;
use std::fmt::Write;
use std::sync::Mutex;

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

struct State<K, V> {
    entries: VecDeque<(K, V)>,
    stats: Stats,
}

pub struct Cache<K, V> {
    capacity: usize,
    state: Mutex<State<K, V>>,
}

impl<K: Eq + std::hash::Hash + Clone, V: Clone> Cache<K, V> {
    pub fn new(capacity: usize) -> Self {
        assert!(capacity > 0, "a cache needs room for at least one entry");
        Self {
            capacity,
            state: Mutex::new(State {
                entries: VecDeque::new(),
                stats: Stats::default(),
            }),
        }
    }

    pub fn get(&self, key: &K) -> Option<V> {
        let mut state = self.state.lock().unwrap();
        let Some(position) = state.entries.iter().position(|(k, _)| k == key) else {
            state.stats.misses += 1;
            return None;
        };
        state.stats.hits += 1;
        let entry = state.entries.remove(position)?;
        let value = entry.1.clone();
        state.entries.push_back(entry);
        Some(value)
    }

    pub fn put(&self, key: K, value: V) {
        let mut state = self.state.lock().unwrap();
        if let Some(position) = state.entries.iter().position(|(k, _)| *k == key) {
            state.entries.remove(position);
        } else if state.entries.len() == self.capacity {
            state.entries.pop_front();
            state.stats.evictions += 1;
        }
        state.entries.push_back((key, value));
    }

    pub fn len(&self) -> usize {
        self.state.lock().unwrap().entries.len()
    }

    pub fn is_empty(&self) -> bool {
        self.len() == 0
    }

    pub fn capacity(&self) -> usize {
        self.capacity
    }

    pub fn stats(&self) -> Stats {
        self.state.lock().unwrap().stats
    }

    pub fn render_metrics(&self, prefix: &str) -> String {
        let stats = self.stats();
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
                self.len() as u64,
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
