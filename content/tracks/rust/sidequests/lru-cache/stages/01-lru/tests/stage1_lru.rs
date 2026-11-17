//! Stage 1 contract: least-recently-used eviction through `&self` methods.

use lru_metrics::Cache;

#[test]
fn a_missing_key_is_none() {
    let cache: Cache<&str, i32> = Cache::new(2);
    assert_eq!(cache.get(&"nope"), None);
}

#[test]
fn a_value_put_can_be_read_back() {
    let cache = Cache::new(2);
    cache.put("a", 1);
    assert_eq!(cache.get(&"a"), Some(1));
    assert_eq!(cache.len(), 1);
    assert!(!cache.is_empty());
}

#[test]
fn a_new_cache_is_empty_and_knows_its_capacity() {
    let cache: Cache<u32, u32> = Cache::new(3);
    assert!(cache.is_empty());
    assert_eq!(cache.len(), 0);
    assert_eq!(cache.capacity(), 3);
}

#[test]
fn the_least_recently_used_entry_is_evicted_first() {
    let cache = Cache::new(2);
    cache.put("a", 1);
    cache.put("b", 2);
    cache.put("c", 3);
    assert_eq!(cache.get(&"a"), None, "a was the oldest");
    assert_eq!(cache.get(&"b"), Some(2));
    assert_eq!(cache.get(&"c"), Some(3));
    assert_eq!(cache.len(), 2);
}

#[test]
fn reading_a_key_makes_it_recent() {
    let cache = Cache::new(2);
    cache.put("a", 1);
    cache.put("b", 2);
    assert_eq!(cache.get(&"a"), Some(1));
    cache.put("c", 3);
    assert_eq!(cache.get(&"b"), None, "b became the least recently used");
    assert_eq!(cache.get(&"a"), Some(1));
}

#[test]
fn replacing_a_value_makes_it_recent_and_evicts_nothing() {
    let cache = Cache::new(2);
    cache.put("a", 1);
    cache.put("b", 2);
    cache.put("a", 10);
    assert_eq!(cache.len(), 2);
    assert_eq!(cache.get(&"b"), Some(2), "nothing was evicted");
    cache.put("c", 3);
    assert_eq!(cache.get(&"a"), None, "a was refreshed before b was read");
    assert_eq!(cache.get(&"b"), Some(2));
    assert_eq!(cache.get(&"c"), Some(3));
}

#[test]
fn a_cache_of_one_keeps_the_last_key() {
    let cache = Cache::new(1);
    cache.put(1, "one");
    cache.put(2, "two");
    assert_eq!(cache.get(&1), None);
    assert_eq!(cache.get(&2), Some("two"));
}

#[test]
fn owned_keys_and_values_work() {
    let cache: Cache<String, Vec<u8>> = Cache::new(2);
    cache.put("k".to_string(), vec![1, 2]);
    assert_eq!(cache.get(&"k".to_string()), Some(vec![1, 2]));
}

#[test]
fn a_long_sequence_keeps_exactly_the_most_recent_keys() {
    let cache = Cache::new(100);
    for key in 0..1_000 {
        cache.put(key, key * 2);
    }
    assert_eq!(cache.len(), 100);
    assert_eq!(cache.get(&899), None);
    for key in 900..1_000 {
        assert_eq!(cache.get(&key), Some(key * 2));
    }
}

#[test]
fn a_capacity_of_zero_panics() {
    let made = std::panic::catch_unwind(|| {
        let _cache: Cache<u8, u8> = Cache::new(0);
    });
    assert!(made.is_err(), "Cache::new(0) should panic");
}
