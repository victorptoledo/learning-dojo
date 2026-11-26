use kata::{count_matching, fields, longer};
use std::sync::Arc;
use std::thread;
use std::time::{Duration, Instant};

#[test]
fn fields_are_trimmed_and_empty_ones_dropped() {
    assert_eq!(fields(" a, b ,,c "), ["a", "b", "c"]);
    assert!(fields(" , ,").is_empty());
    assert!(fields("").is_empty());
}

#[test]
fn fields_point_into_the_line() {
    let line = String::from("north, south , east");
    let range = line.as_bytes().as_ptr_range();
    for field in fields(&line) {
        assert!(range.contains(&field.as_ptr()), "`{field}` was copied");
    }
}

#[test]
fn longer_counts_characters_and_keeps_the_first_on_a_tie() {
    assert_eq!(longer("ab", "abc"), "abc");
    assert_eq!(longer("same", "size"), "same");
    assert_eq!(longer("ééé", "abcd"), "abcd");
}

#[test]
fn longer_works_with_slices_of_different_strings() {
    let outer = String::from("outer text");
    let result;
    {
        let inner = String::from("in");
        result = longer(&outer, &inner).to_string();
    }
    assert_eq!(result, "outer text");
}

#[test]
fn counts_every_matching_item() {
    let items: Vec<String> = (0..100).map(|n| n.to_string()).collect();
    let even = Arc::new(|item: &str| item.parse::<u32>().unwrap() % 2 == 0);
    assert_eq!(count_matching(items.clone(), 3, even.clone()), 50);
    assert_eq!(count_matching(items, 0, even), 50);
}

#[test]
fn no_items_count_zero() {
    assert_eq!(count_matching(vec![], 4, Arc::new(|_: &str| true)), 0);
}

#[test]
fn more_workers_than_items_is_fine() {
    let items = vec!["a".to_string(), "b".to_string()];
    assert_eq!(count_matching(items, 8, Arc::new(|_: &str| true)), 2);
}

#[test]
fn items_are_tested_in_parallel() {
    let items: Vec<String> = (0..4).map(|n| n.to_string()).collect();
    let slow = Arc::new(|_: &str| {
        thread::sleep(Duration::from_millis(250));
        true
    });
    let begin = Instant::now();
    assert_eq!(count_matching(items, 4, slow), 4);
    assert!(begin.elapsed() < Duration::from_millis(750), "took {:?}", begin.elapsed());
}
