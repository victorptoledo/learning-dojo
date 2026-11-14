use kata::{top_words, word_counts, words};

#[test]
fn inner_apostrophes_stay_and_punctuation_only_pieces_go() {
    assert_eq!(words("Don't -- STOP... now"), ["don't", "stop", "now"]);
}

#[test]
fn empty_and_blank_texts_have_no_words() {
    assert!(words("").is_empty());
    assert!(words("  \n\t ").is_empty());
    assert!(word_counts("?! ...").is_empty());
}

#[test]
fn counts_ignore_case_and_punctuation() {
    let counts = word_counts("Rust, rust; RUST! ferris");
    assert_eq!(counts.len(), 2);
    assert_eq!(counts["rust"], 3);
    assert_eq!(counts["ferris"], 1);
}

#[test]
fn unicode_words_are_kept() {
    assert_eq!(words("Olá, mundo! Olá."), ["olá", "mundo", "olá"]);
}

#[test]
fn ties_are_broken_alphabetically() {
    assert_eq!(
        top_words("pear apple pear apple fig", 3),
        [("apple".to_string(), 2), ("pear".to_string(), 2), ("fig".to_string(), 1)]
    );
}

#[test]
fn asking_for_more_than_exists_returns_everything() {
    assert_eq!(top_words("one two", 10).len(), 2);
    assert!(top_words("", 3).is_empty());
    assert!(top_words("a b", 0).is_empty());
}
