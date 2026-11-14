use std::collections::HashMap;

/// The words of `text`, lowercased, without surrounding punctuation.
pub fn words(text: &str) -> Vec<String> {
    todo!("split {text:?} into words")
}

/// How many times each word appears in `text`.
pub fn word_counts(text: &str) -> HashMap<String, usize> {
    todo!("count the words of {text:?}")
}

/// The `n` most frequent words, highest count first, alphabetical on a tie.
pub fn top_words(text: &str, n: usize) -> Vec<(String, usize)> {
    todo!("top {n} words of {text:?}")
}
