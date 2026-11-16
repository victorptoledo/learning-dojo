use kata::{chunk_bounds, parallel_longest, parallel_sum};

#[test]
fn larger_chunks_come_first() {
    assert_eq!(chunk_bounds(10, 3), [0..4, 4..7, 7..10]);
}

#[test]
fn never_more_chunks_than_items() {
    assert_eq!(chunk_bounds(2, 5), [0..1, 1..2]);
}

#[test]
fn no_items_no_chunks() {
    assert!(chunk_bounds(0, 4).is_empty());
}

#[test]
fn zero_parts_counts_as_one() {
    assert_eq!(chunk_bounds(5, 0), [0..5]);
}

#[test]
fn chunks_cover_everything_once() {
    for len in 0..40 {
        for parts in 0..9 {
            let chunks = chunk_bounds(len, parts);
            let covered: Vec<usize> = chunks.iter().cloned().flatten().collect();
            assert_eq!(covered, (0..len).collect::<Vec<_>>(), "len {len}, parts {parts}");
            let sizes: Vec<usize> = chunks.iter().map(|range| range.len()).collect();
            if let (Some(max), Some(min)) = (sizes.iter().max(), sizes.iter().min()) {
                assert!(max - min <= 1, "unbalanced {sizes:?}");
                assert!(*min > 0, "empty chunk in {sizes:?}");
            }
        }
    }
}

#[test]
fn the_sum_does_not_depend_on_the_thread_count() {
    let values: Vec<u64> = (1..=1000).collect();
    for threads in [0, 1, 4, 7, 2000] {
        assert_eq!(parallel_sum(&values, threads), 500_500, "{threads} threads");
    }
}

#[test]
fn the_sum_of_nothing_is_zero() {
    assert_eq!(parallel_sum(&[], 4), 0);
}

#[test]
fn longest_word_in_characters() {
    assert_eq!(parallel_longest(&["a", "héllo", "abcd"], 2), Some(5));
    assert_eq!(parallel_longest(&["one"], 8), Some(3));
}

#[test]
fn no_words_no_length() {
    assert_eq!(parallel_longest(&[], 3), None);
}

#[test]
fn works_on_borrowed_local_data() {
    let text = String::from("scoped threads borrow local data safely");
    let words: Vec<&str> = text.split_whitespace().collect();
    assert_eq!(parallel_longest(&words, 3), Some(7));
}
