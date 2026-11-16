use std::time::{Duration, Instant};

use kata::{parse_trace, simulate, Access, Cache, CacheConfig, Outcome, Stats};

fn cache(sets: usize, ways: usize, line_size: usize) -> Cache {
    Cache::new(CacheConfig { sets, ways, line_size }).expect("a valid configuration")
}

#[test]
fn invalid_configurations_are_rejected() {
    for (sets, ways, line_size) in [(3, 1, 64), (0, 1, 64), (4, 0, 64), (4, 2, 48), (4, 2, 0)] {
        assert!(
            Cache::new(CacheConfig { sets, ways, line_size }).is_err(),
            "sets={sets} ways={ways} line_size={line_size} should be rejected"
        );
    }
    assert!(Cache::new(CacheConfig { sets: 1, ways: 3, line_size: 1 }).is_ok());
}

#[test]
fn split_uses_the_right_number_of_bits() {
    let l1 = cache(64, 8, 64);
    assert_eq!(l1.split(0), (0, 0, 0));
    assert_eq!(l1.split(63), (0, 0, 63));
    assert_eq!(l1.split(64), (0, 1, 0));
    assert_eq!(l1.split(4095), (0, 63, 63));
    assert_eq!(l1.split(4096), (1, 0, 0));
    assert_eq!(l1.split(0xDEAD_BEEF), (0xDEAD_BEEF >> 12, (0xDEAD_BEEF >> 6) & 63, 0x2F));
    let fully = cache(1, 4, 32);
    assert_eq!(fully.split(0x1234), (0x1234 >> 5, 0, 0x14));
}

#[test]
fn direct_mapped_conflicts_evict_each_other() {
    let mut c = cache(4, 1, 16);
    // 0x000 and 0x040 both map to set 0
    assert_eq!(c.access(Access::Read(0x000)), Outcome::Miss);
    assert_eq!(
        c.access(Access::Read(0x040)),
        Outcome::Eviction { line: 0x000, dirty: false }
    );
    assert_eq!(
        c.access(Access::Read(0x008)),
        Outcome::Eviction { line: 0x040, dirty: false }
    );
    assert_eq!(c.stats(), Stats { hits: 0, misses: 3, evictions: 2, writebacks: 0 });
}

#[test]
fn two_ways_remove_the_same_conflict() {
    let mut c = cache(2, 2, 16);
    for _ in 0..3 {
        c.access(Access::Read(0x000));
        c.access(Access::Read(0x040));
    }
    assert_eq!(c.stats(), Stats { hits: 4, misses: 2, evictions: 0, writebacks: 0 });
}

#[test]
fn lru_evicts_the_least_recently_used_line() {
    let mut c = cache(1, 3, 16);
    c.access(Access::Read(0x00)); // A
    c.access(Access::Read(0x10)); // B
    c.access(Access::Read(0x20)); // C
    assert_eq!(c.access(Access::Read(0x00)), Outcome::Hit); // A is now the most recent
    assert_eq!(
        c.access(Access::Read(0x30)),
        Outcome::Eviction { line: 0x10, dirty: false }
    ); // B was the least recent
    assert_eq!(
        c.access(Access::Read(0x40)),
        Outcome::Eviction { line: 0x20, dirty: false }
    );
    assert_eq!(c.access(Access::Read(0x00)), Outcome::Hit);
}

#[test]
fn evicted_line_addresses_are_rebuilt_from_tag_and_index() {
    let mut c = cache(8, 1, 64);
    c.access(Access::Read(0x12_3456));
    let (tag, index, _) = c.split(0x12_3456);
    let other = (tag + 1) << 9 | (index as u64) << 6;
    assert_eq!(
        c.access(Access::Read(other)),
        Outcome::Eviction { line: 0x12_3440, dirty: false }
    );
}

#[test]
fn writes_make_lines_dirty_and_evictions_write_them_back() {
    let mut c = cache(1, 1, 16);
    assert_eq!(c.access(Access::Write(0x00)), Outcome::Miss); // write-allocate
    assert_eq!(c.access(Access::Read(0x04)), Outcome::Hit);
    assert_eq!(
        c.access(Access::Read(0x10)),
        Outcome::Eviction { line: 0x00, dirty: true }
    );
    assert_eq!(c.access(Access::Read(0x14)), Outcome::Hit);
    assert_eq!(
        c.access(Access::Write(0x20)),
        Outcome::Eviction { line: 0x10, dirty: false }
    );
    assert_eq!(c.stats(), Stats { hits: 2, misses: 3, evictions: 2, writebacks: 1 });
}

#[test]
fn a_write_hit_makes_a_clean_line_dirty() {
    let mut c = cache(1, 1, 16);
    c.access(Access::Read(0x00));
    assert_eq!(c.access(Access::Write(0x08)), Outcome::Hit);
    assert_eq!(
        c.access(Access::Read(0x40)),
        Outcome::Eviction { line: 0x00, dirty: true }
    );
}

#[test]
fn flush_writes_back_dirty_lines_and_empties_the_cache() {
    let mut c = cache(4, 2, 16);
    c.access(Access::Write(0x00));
    c.access(Access::Write(0x10));
    c.access(Access::Read(0x20));
    assert_eq!(c.flush(), 2);
    assert_eq!(c.stats().writebacks, 2);
    assert_eq!(c.flush(), 0);
    assert_eq!(c.access(Access::Read(0x00)), Outcome::Miss);
}

#[test]
fn parse_trace_reads_reads_writes_comments_and_hex() {
    let trace = "# a comment\nR 0x10\n\n  W 1F  \nr 0XfF\nw 0\n";
    assert_eq!(
        parse_trace(trace).unwrap(),
        vec![Access::Read(0x10), Access::Write(0x1F), Access::Read(0xFF), Access::Write(0)]
    );
}

#[test]
fn parse_trace_errors_name_the_line() {
    for (trace, line) in [
        ("R 0x10\nX 0x10", "line 2"),
        ("R 0x10\nR\n", "line 2"),
        ("\n\nR zz", "line 3"),
        ("R 0x10 extra", "line 1"),
        ("W 0x", "line 1"),
    ] {
        let error = parse_trace(trace).expect_err(trace);
        assert!(error.contains(line), "error `{error}` should mention {line}");
    }
}

/// A row-major walk over a rows × cols matrix of 4-byte ints starting at `base`.
fn matrix_trace(rows: u64, cols: u64, by_row: bool) -> String {
    let mut out = String::new();
    for i in 0..rows {
        for j in 0..cols {
            let (r, c) = if by_row { (i, j) } else { (j, i) };
            out.push_str(&format!("R {:x}\n", 0x10_0000 + (r * cols + c) * 4));
        }
    }
    out
}

#[test]
fn row_major_walk_misses_once_per_line() {
    let l1 = CacheConfig { sets: 64, ways: 8, line_size: 64 };
    let stats = simulate(l1, &matrix_trace(256, 256, true)).unwrap();
    assert_eq!(stats.misses, 256 * 256 / 16);
    assert_eq!(stats.hits, 256 * 256 - 256 * 256 / 16);
}

#[test]
fn column_walk_misses_on_every_access() {
    let l1 = CacheConfig { sets: 64, ways: 8, line_size: 64 };
    let stats = simulate(l1, &matrix_trace(256, 256, false)).unwrap();
    assert_eq!(stats.misses, 256 * 256);
    assert_eq!(stats.hits, 0);
}

#[test]
fn simulate_flushes_at_the_end_and_reports_errors() {
    let config = CacheConfig { sets: 2, ways: 1, line_size: 16 };
    let stats = simulate(config, "W 0\nW 10\nR 0\n").unwrap();
    assert_eq!(stats, Stats { hits: 1, misses: 2, evictions: 0, writebacks: 2 });
    assert!(simulate(config, "R nope").is_err());
    assert!(simulate(CacheConfig { sets: 3, ways: 1, line_size: 16 }, "R 0").is_err());
}

#[test]
fn matches_a_simple_model_on_a_random_trace() {
    // A slow but obviously correct model: one Vec per set, most recent first.
    let config = CacheConfig { sets: 16, ways: 4, line_size: 32 };
    let mut fast = Cache::new(config).unwrap();
    let mut model: Vec<Vec<(u64, bool)>> = vec![Vec::new(); 16];
    let mut state = 0x2545_F491_4F6C_DD1Du64;
    for _ in 0..20_000 {
        state ^= state << 13;
        state ^= state >> 7;
        state ^= state << 17;
        let address = state % 8192;
        let write = state & 0x100 != 0;
        let line = address / 32;
        let (set, tag) = ((line % 16) as usize, line / 16);
        let expected = if let Some(p) = model[set].iter().position(|&(t, _)| t == tag) {
            let (t, d) = model[set].remove(p);
            model[set].insert(0, (t, d || write));
            Outcome::Hit
        } else {
            let mut outcome = Outcome::Miss;
            if model[set].len() == 4 {
                let (t, d) = model[set].pop().unwrap();
                outcome = Outcome::Eviction { line: (t * 16 + set as u64) * 32, dirty: d };
            }
            model[set].insert(0, (tag, write));
            outcome
        };
        let access = if write { Access::Write(address) } else { Access::Read(address) };
        assert_eq!(fast.access(access), expected, "access {access:?}");
    }
}

#[test]
fn a_million_accesses_run_quickly() {
    let mut c = cache(1024, 8, 64);
    let start = Instant::now();
    let mut address = 0u64;
    for i in 0..1_000_000u64 {
        address = address.wrapping_mul(6364136223846793005).wrapping_add(1442695040888963407);
        let access = if i % 3 == 0 { Access::Write(address >> 40) } else { Access::Read(address >> 40) };
        c.access(access);
    }
    let stats = c.stats();
    assert_eq!(stats.hits + stats.misses, 1_000_000);
    assert!(start.elapsed() < Duration::from_secs(5), "took {:?}", start.elapsed());
}
