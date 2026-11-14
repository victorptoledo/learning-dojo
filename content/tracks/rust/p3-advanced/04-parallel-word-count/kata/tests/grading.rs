use kata::{chunk_ranges, count_words, merge, parallel_count};
use std::collections::HashMap;

fn sequential(texts: &[&str]) -> HashMap<String, usize> {
    let mut total = HashMap::new();
    for text in texts {
        merge(&mut total, count_words(text));
    }
    total
}

#[test]
fn merge_adds_counts_and_keeps_new_words() {
    let mut into = HashMap::from([("a".to_string(), 2)]);
    merge(&mut into, HashMap::from([("a".to_string(), 3), ("b".to_string(), 1)]));
    assert_eq!(into, HashMap::from([("a".to_string(), 5), ("b".to_string(), 1)]));
}

#[test]
fn words_follow_the_word_counter_rules() {
    let counts = count_words("Don't -- STOP... don't");
    assert_eq!(counts.len(), 2);
    assert_eq!(counts["don't"], 2);
    assert_eq!(counts["stop"], 1);
}

#[test]
fn ranges_are_balanced_contiguous_and_never_empty() {
    assert_eq!(chunk_ranges(2, 5), vec![0..1, 1..2]);
    assert_eq!(chunk_ranges(9, 3), vec![0..3, 3..6, 6..9]);
    assert_eq!(chunk_ranges(7, 1), vec![0..7]);
    assert_eq!(chunk_ranges(5, 0), vec![0..5]);
    assert!(chunk_ranges(0, 4).is_empty());
    for len in 0..40 {
        for workers in 0..9 {
            let ranges = chunk_ranges(len, workers);
            assert_eq!(ranges.len(), len.min(workers.max(1)));
            let mut next = 0;
            for range in &ranges {
                assert_eq!(range.start, next, "contiguous for {len}/{workers}");
                assert!(!range.is_empty());
                next = range.end;
            }
            assert_eq!(next, len);
            let sizes: Vec<usize> = ranges.iter().map(|r| r.len()).collect();
            assert!(sizes.windows(2).all(|w| w[0] >= w[1] && w[0] - w[1] <= 1), "{sizes:?}");
        }
    }
}

#[test]
fn parallel_equals_sequential_for_any_worker_count() {
    let owned: Vec<String> = (0..50)
        .map(|i| format!("word{} common Shared, shared! line{}", i % 7, i))
        .collect();
    let texts: Vec<&str> = owned.iter().map(String::as_str).collect();
    let expected = sequential(&texts);
    for workers in [0, 1, 2, 3, 8, 64] {
        assert_eq!(parallel_count(&texts, workers), expected, "{workers} workers");
    }
    assert_eq!(expected["shared"], 100);
}

#[test]
fn no_texts_means_no_words() {
    assert!(parallel_count(&[], 4).is_empty());
    assert!(parallel_count(&["", "  "], 2).is_empty());
}
