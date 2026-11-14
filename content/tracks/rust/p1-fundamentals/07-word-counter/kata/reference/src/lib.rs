use std::collections::HashMap;

/// The words of `text`, lowercased, without surrounding punctuation.
pub fn words(text: &str) -> Vec<String> {
    text.split_whitespace()
        .map(|piece| piece.trim_matches(|c: char| !c.is_alphanumeric()))
        .filter(|word| !word.is_empty())
        .map(str::to_lowercase)
        .collect()
}

/// How many times each word appears in `text`.
pub fn word_counts(text: &str) -> HashMap<String, usize> {
    let mut counts = HashMap::new();
    for word in words(text) {
        *counts.entry(word).or_insert(0) += 1;
    }
    counts
}

/// The `n` most frequent words, highest count first, alphabetical on a tie.
pub fn top_words(text: &str, n: usize) -> Vec<(String, usize)> {
    let mut ranked: Vec<(String, usize)> = word_counts(text).into_iter().collect();
    ranked.sort_by(|a, b| b.1.cmp(&a.1).then_with(|| a.0.cmp(&b.0)));
    ranked.truncate(n);
    ranked
}
