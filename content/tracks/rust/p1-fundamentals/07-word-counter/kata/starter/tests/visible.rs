use kata::{top_words, word_counts, words};

#[test]
fn words_are_lowercased_and_trimmed() {
    assert_eq!(words("Hello, Dojo!"), ["hello", "dojo"]);
}

#[test]
fn counts_repeated_words() {
    let counts = word_counts("a b a");
    assert_eq!(counts["a"], 2);
    assert_eq!(counts["b"], 1);
}

#[test]
fn top_word_of_a_sentence() {
    assert_eq!(top_words("the cat and the hat", 1), [("the".to_string(), 2)]);
}
