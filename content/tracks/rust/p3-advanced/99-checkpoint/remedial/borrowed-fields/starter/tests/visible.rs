use kata::{count_matching, fields, longer};
use std::sync::Arc;

#[test]
fn splits_and_trims() {
    assert_eq!(fields(" a, b ,,c "), ["a", "b", "c"]);
}

#[test]
fn picks_the_longer() {
    assert_eq!(longer("dojo", "kata!"), "kata!");
}

#[test]
fn counts_on_two_threads() {
    let items = vec!["ab".to_string(), "abc".into(), "x".into()];
    assert_eq!(count_matching(items, 2, Arc::new(|item: &str| item.len() > 1)), 2);
}
