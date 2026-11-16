# Cache Simulator

Build the cache of the previous lesson in `src/lib.rs`: a **set-associative** cache with **LRU**
replacement, **write-back** and **write-allocate**, then run memory traces through it. The types
are already declared; you write the bodies (and the fields of `Cache`).

## The cache

`Cache::new(CacheConfig { sets, ways, line_size })` creates an empty (cold) cache. `sets` and
`line_size` must be powers of two and `ways` at least 1; otherwise return `Err` with a message.

| Method | Does |
| --- | --- |
| `split(address) -> (tag, index, offset)` | the low log₂(line_size) bits are the offset, the next log₂(sets) bits the set index, the rest the tag |
| `access(Access::Read(a) \| Access::Write(a)) -> Outcome` | looks the line up in its set and updates the statistics |
| `stats() -> Stats` | `hits`, `misses`, `evictions` and `writebacks` so far |
| `flush() -> u64` | writes back every dirty line (counted in `writebacks`), empties the cache and returns how many were written |

An access returns:

- `Outcome::Hit` when the line is in its set. It becomes the **most recently used** line of the
  set, and a write marks it **dirty**.
- `Outcome::Miss` when the line is not there and the set still has a free way: the line is
  loaded (a write loads it too, and marks it dirty).
- `Outcome::Eviction { line, dirty }` when the set is full: the **least recently used** line is
  evicted, `line` is the address of its first byte (rebuilt from its tag and the set index), and
  `dirty` says whether it had to be written back (count it in `writebacks`). Then the new line is
  loaded as above.

```rust
let mut c = Cache::new(CacheConfig { sets: 1, ways: 1, line_size: 16 })?;
c.access(Access::Write(0x00));   // Miss: loaded and dirty
c.access(Access::Read(0x04));    // Hit: same 16-byte line
c.access(Access::Read(0x10));    // Eviction { line: 0x00, dirty: true }
```

## Traces

`parse_trace(text)` reads one access per line: `R <address>` or `W <address>` (either case), the
address in hexadecimal with or without `0x`. Blank lines and lines starting with `#` are skipped.
Anything else is an `Err` whose message contains `line N` (counting from 1).

`simulate(config, trace)` runs a whole trace on a new cache, **flushes** it at the end and returns
the statistics.

## What the hidden tests check

- Bad configurations, the address split for several shapes, direct-mapped conflicts, and LRU
  order in a set.
- Dirty lines: write-allocate, write hits, the `dirty` flag of evictions and `flush`.
- A 256 × 256 matrix of ints walked row by row misses once per 64-byte line; walked column by
  column on the same 32 KiB, 8-way cache, it misses on every access.
- 20,000 random accesses against a simple model, and a million accesses within a time limit.

Run the visible tests with `cargo test`.

> Tip: keep each set as a small `Vec` of lines ordered from most to least recently used. A hit
> moves its line to the front, a miss inserts at the front and, if the set is full, pops the
> back. With 8 ways that is cheap enough.
