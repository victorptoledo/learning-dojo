//! A set-associative, write-back, write-allocate cache with LRU replacement.

/// The shape of a cache: `sets` sets of `ways` lines of `line_size` bytes.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct CacheConfig {
    pub sets: usize,
    pub ways: usize,
    pub line_size: usize,
}

/// One memory access of a trace.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Access {
    Read(u64),
    Write(u64),
}

/// What one access did to the cache.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Outcome {
    /// The line was in the cache.
    Hit,
    /// The line was fetched into a free way.
    Miss,
    /// The line was fetched and the least recently used line of the set was evicted.
    /// `line` is the address of the first byte of the evicted line.
    Eviction { line: u64, dirty: bool },
}

/// Counters over all accesses since the cache was created.
#[derive(Debug, Default, Clone, Copy, PartialEq, Eq)]
pub struct Stats {
    pub hits: u64,
    pub misses: u64,
    pub evictions: u64,
    pub writebacks: u64,
}

pub struct Cache {
    // Add the fields you need: the configuration, the sets with their lines (tag, dirty,
    // and how recently each was used) and the statistics.
    config: CacheConfig,
}

impl Cache {
    /// A cold cache. `sets` and `line_size` must be powers of two and `ways` at least 1.
    pub fn new(config: CacheConfig) -> Result<Cache, String> {
        todo!("validate {config:?} and build an empty cache")
    }

    /// The (tag, set index, offset) fields of an address.
    pub fn split(&self, address: u64) -> (u64, usize, usize) {
        todo!("split {address:#x} with {:?}", self.config)
    }

    pub fn access(&mut self, access: Access) -> Outcome {
        todo!("look up {access:?}, update LRU order, dirty bits and the statistics")
    }

    pub fn stats(&self) -> Stats {
        todo!("return the statistics")
    }

    /// Writes back every dirty line and empties the cache. Returns the number of write-backs.
    pub fn flush(&mut self) -> u64 {
        todo!("write back the dirty lines")
    }
}

/// Parses a trace: one `R <address>` or `W <address>` per line, the address in hexadecimal
/// with or without `0x`. Blank lines and lines starting with `#` are skipped.
pub fn parse_trace(text: &str) -> Result<Vec<Access>, String> {
    todo!("parse {} lines", text.lines().count())
}

/// Runs a whole trace on a cold cache and flushes it at the end.
pub fn simulate(config: CacheConfig, trace: &str) -> Result<Stats, String> {
    todo!("run {} bytes of trace on {config:?}", trace.len())
}
