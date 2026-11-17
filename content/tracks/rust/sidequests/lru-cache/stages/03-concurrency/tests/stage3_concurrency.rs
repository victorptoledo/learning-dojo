//! Stage 3 contract: one cache shared by many threads, and get_or_insert_with.

use lru_metrics::Cache;
use std::sync::Arc;
use std::sync::Barrier;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::thread;
use std::time::Duration;

fn assert_send_sync<T: Send + Sync>() {}

#[test]
fn the_cache_is_send_and_sync() {
    assert_send_sync::<Cache<String, Vec<u8>>>();
    assert_send_sync::<Cache<u64, String>>();
}

#[test]
fn many_threads_keep_the_counts_exact_and_the_size_bounded() {
    let cache = Arc::new(Cache::new(64));
    let threads = 8;
    let rounds = 4_000u64;
    let handles: Vec<_> = (0..threads)
        .map(|t| {
            let cache = Arc::clone(&cache);
            thread::spawn(move || {
                for i in 0..rounds {
                    let key = (i * 7 + t) % 200;
                    if i % 3 == 0 {
                        cache.put(key, i);
                    } else {
                        cache.get(&key);
                    }
                    assert!(cache.len() <= 64);
                }
            })
        })
        .collect();
    for handle in handles {
        handle.join().expect("no thread panicked");
    }
    let gets = threads * (rounds - rounds.div_ceil(3));
    let stats = cache.stats();
    assert_eq!(
        stats.hits + stats.misses,
        gets,
        "every get is a hit or a miss"
    );
    assert!(cache.len() <= 64);
}

#[test]
fn get_or_insert_with_computes_a_missing_value() {
    let cache = Cache::new(2);
    assert_eq!(cache.get_or_insert_with("a", || 1), 1);
    assert_eq!(
        cache.get_or_insert_with("a", || 99),
        1,
        "a hit keeps the value"
    );
    assert_eq!(cache.get(&"a"), Some(1));
    let stats = cache.stats();
    assert_eq!((stats.hits, stats.misses), (2, 1));
}

#[test]
fn concurrent_callers_compute_a_key_only_once() {
    let cache = Arc::new(Cache::new(8));
    let calls = Arc::new(AtomicUsize::new(0));
    let start = Arc::new(Barrier::new(8));
    let handles: Vec<_> = (0..8)
        .map(|_| {
            let (cache, calls, start) =
                (Arc::clone(&cache), Arc::clone(&calls), Arc::clone(&start));
            thread::spawn(move || {
                start.wait();
                cache.get_or_insert_with("report", || {
                    calls.fetch_add(1, Ordering::SeqCst);
                    thread::sleep(Duration::from_millis(50));
                    42
                })
            })
        })
        .collect();
    for handle in handles {
        assert_eq!(handle.join().unwrap(), 42);
    }
    assert_eq!(
        calls.load(Ordering::SeqCst),
        1,
        "the value was computed once"
    );
    let stats = cache.stats();
    assert_eq!((stats.hits, stats.misses), (7, 1));
}

#[test]
fn get_or_insert_with_evicts_like_put() {
    let cache = Cache::new(1);
    cache.get_or_insert_with(1, || "one");
    cache.get_or_insert_with(2, || "two");
    assert_eq!(cache.get(&1), None);
    assert_eq!(cache.stats().evictions, 1);
}
