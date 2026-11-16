use kata::{chunk_bounds, parallel_sum};

#[test]
fn two_even_chunks() {
    assert_eq!(chunk_bounds(4, 2), [0..2, 2..4]);
}

#[test]
fn sums_on_two_threads() {
    assert_eq!(parallel_sum(&[1, 2, 3, 4], 2), 10);
}
