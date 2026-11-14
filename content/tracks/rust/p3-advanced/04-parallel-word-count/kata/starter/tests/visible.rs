use kata::{chunk_ranges, count_words, parallel_count};

#[test]
fn counts_one_text() {
    let counts = count_words("The cat, the hat.");
    assert_eq!(counts["the"], 2);
    assert_eq!(counts["cat"], 1);
}

#[test]
fn splits_ten_items_for_three_workers() {
    assert_eq!(chunk_ranges(10, 3), vec![0..4, 4..7, 7..10]);
}

#[test]
fn parallel_counts_add_up() {
    let counts = parallel_count(&["a b", "b c", "c c"], 2);
    assert_eq!(counts["c"], 3);
}
