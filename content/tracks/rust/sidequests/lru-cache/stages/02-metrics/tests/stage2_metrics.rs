//! Stage 2 contract: hit/miss/eviction counts and the Prometheus text format.

use lru_metrics::{Cache, Stats};

fn sample(text: &str, name: &str) -> Option<f64> {
    text.lines()
        .filter(|line| !line.starts_with('#'))
        .find_map(|line| {
            let (metric, value) = line.split_once(' ')?;
            (metric == name).then(|| value.trim().parse().ok())?
        })
}

#[test]
fn a_new_cache_has_zero_stats_and_a_zero_ratio() {
    let cache: Cache<u8, u8> = Cache::new(2);
    assert_eq!(cache.stats(), Stats::default());
    assert_eq!(cache.stats().hit_ratio(), 0.0);
}

#[test]
fn gets_count_hits_and_misses_and_puts_count_neither() {
    let cache = Cache::new(2);
    cache.put("a", 1);
    cache.put("a", 2);
    cache.get(&"a");
    cache.get(&"a");
    cache.get(&"a");
    cache.get(&"zzz");
    let stats = cache.stats();
    assert_eq!((stats.hits, stats.misses), (3, 1));
    assert_eq!(stats.hit_ratio(), 0.75);
}

#[test]
fn evictions_are_counted_but_replacements_are_not() {
    let cache = Cache::new(2);
    cache.put(1, 1);
    cache.put(2, 2);
    cache.put(2, 20);
    assert_eq!(cache.stats().evictions, 0);
    cache.put(3, 3);
    cache.put(4, 4);
    assert_eq!(cache.stats().evictions, 2);
}

#[test]
fn metrics_have_help_type_and_value_for_every_series() {
    let cache = Cache::new(2);
    cache.put("a", 1);
    cache.put("b", 2);
    cache.get(&"a");
    cache.get(&"a");
    cache.get(&"a");
    cache.get(&"x");
    let text = cache.render_metrics("cache");
    for (name, kind, value) in [
        ("cache_hits_total", "counter", 3.0),
        ("cache_misses_total", "counter", 1.0),
        ("cache_evictions_total", "counter", 0.0),
        ("cache_entries", "gauge", 2.0),
        ("cache_capacity", "gauge", 2.0),
    ] {
        assert!(
            text.lines()
                .any(|line| line.starts_with(&format!("# HELP {name} "))),
            "no HELP line for {name} in:\n{text}"
        );
        assert!(
            text.lines()
                .any(|line| line == format!("# TYPE {name} {kind}")),
            "no `# TYPE {name} {kind}` line in:\n{text}"
        );
        assert_eq!(
            sample(&text, name),
            Some(value),
            "value of {name} in:\n{text}"
        );
    }
}

#[test]
fn the_prefix_names_every_metric() {
    let cache: Cache<u8, u8> = Cache::new(4);
    cache.put(1, 1);
    let text = cache.render_metrics("sessions");
    assert_eq!(sample(&text, "sessions_entries"), Some(1.0), "{text}");
    assert_eq!(sample(&text, "sessions_capacity"), Some(4.0), "{text}");
    assert!(
        text.lines()
            .filter(|line| !line.is_empty())
            .all(|line| line.contains("sessions_")),
        "every line names a sessions_ metric:\n{text}"
    );
}

#[test]
fn counters_follow_the_cache_over_time() {
    let cache = Cache::new(1);
    cache.put("a", 1);
    cache.put("b", 2);
    cache.get(&"a");
    let text = cache.render_metrics("cache");
    assert_eq!(sample(&text, "cache_evictions_total"), Some(1.0));
    assert_eq!(sample(&text, "cache_misses_total"), Some(1.0));
    assert_eq!(sample(&text, "cache_entries"), Some(1.0));
}
