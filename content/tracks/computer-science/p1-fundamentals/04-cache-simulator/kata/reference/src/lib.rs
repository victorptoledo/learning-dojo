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

#[derive(Debug, Clone, Copy)]
struct Line {
    tag: u64,
    dirty: bool,
}

pub struct Cache {
    config: CacheConfig,
    offset_bits: u32,
    index_bits: u32,
    /// Each set keeps its lines from most to least recently used.
    sets: Vec<Vec<Line>>,
    stats: Stats,
}

impl Cache {
    /// A cold cache. `sets` and `line_size` must be powers of two and `ways` at least 1.
    pub fn new(config: CacheConfig) -> Result<Cache, String> {
        if !config.sets.is_power_of_two() {
            return Err(format!("sets must be a power of two, got {}", config.sets));
        }
        if !config.line_size.is_power_of_two() {
            return Err(format!(
                "line_size must be a power of two, got {}",
                config.line_size
            ));
        }
        if config.ways == 0 {
            return Err("ways must be at least 1".to_string());
        }
        Ok(Cache {
            config,
            offset_bits: config.line_size.trailing_zeros(),
            index_bits: config.sets.trailing_zeros(),
            sets: vec![Vec::with_capacity(config.ways); config.sets],
            stats: Stats::default(),
        })
    }

    /// The (tag, set index, offset) fields of an address.
    pub fn split(&self, address: u64) -> (u64, usize, usize) {
        let offset = address & (self.config.line_size as u64 - 1);
        let index = (address >> self.offset_bits) & (self.config.sets as u64 - 1);
        let tag = address >> (self.offset_bits + self.index_bits);
        (tag, index as usize, offset as usize)
    }

    pub fn access(&mut self, access: Access) -> Outcome {
        let (address, write) = match access {
            Access::Read(address) => (address, false),
            Access::Write(address) => (address, true),
        };
        let (tag, index, _) = self.split(address);
        let set = &mut self.sets[index];
        if let Some(position) = set.iter().position(|line| line.tag == tag) {
            let mut line = set.remove(position);
            line.dirty |= write;
            set.insert(0, line);
            self.stats.hits += 1;
            return Outcome::Hit;
        }
        self.stats.misses += 1;
        let mut outcome = Outcome::Miss;
        if set.len() == self.config.ways {
            let victim = set.pop().expect("a full set has a line");
            self.stats.evictions += 1;
            if victim.dirty {
                self.stats.writebacks += 1;
            }
            let line = ((victim.tag << self.index_bits) | index as u64) << self.offset_bits;
            outcome = Outcome::Eviction {
                line,
                dirty: victim.dirty,
            };
        }
        set.insert(0, Line { tag, dirty: write });
        outcome
    }

    pub fn stats(&self) -> Stats {
        self.stats
    }

    /// Writes back every dirty line and empties the cache. Returns the number of write-backs.
    pub fn flush(&mut self) -> u64 {
        let mut written = 0;
        for set in &mut self.sets {
            written += set.iter().filter(|line| line.dirty).count() as u64;
            set.clear();
        }
        self.stats.writebacks += written;
        written
    }
}

/// Parses a trace: one `R <address>` or `W <address>` per line, the address in hexadecimal
/// with or without `0x`. Blank lines and lines starting with `#` are skipped.
pub fn parse_trace(text: &str) -> Result<Vec<Access>, String> {
    let mut accesses = Vec::new();
    for (number, line) in text.lines().enumerate() {
        let line = line.trim();
        if line.is_empty() || line.starts_with('#') {
            continue;
        }
        let bad = || format!("line {}: expected `R <hex>` or `W <hex>`, got `{line}`", number + 1);
        let mut parts = line.split_whitespace();
        let (Some(kind), Some(address), None) = (parts.next(), parts.next(), parts.next()) else {
            return Err(bad());
        };
        let digits = address
            .strip_prefix("0x")
            .or_else(|| address.strip_prefix("0X"))
            .unwrap_or(address);
        let address = u64::from_str_radix(digits, 16).map_err(|_| bad())?;
        accesses.push(match kind {
            "R" | "r" => Access::Read(address),
            "W" | "w" => Access::Write(address),
            _ => return Err(bad()),
        });
    }
    Ok(accesses)
}

/// Runs a whole trace on a cold cache and flushes it at the end.
pub fn simulate(config: CacheConfig, trace: &str) -> Result<Stats, String> {
    let mut cache = Cache::new(config)?;
    for access in parse_trace(trace)? {
        cache.access(access);
    }
    cache.flush();
    Ok(cache.stats())
}
