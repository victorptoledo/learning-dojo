use std::collections::{BTreeSet, HashMap};

/// Letter counts, most frequent first, ties in alphabetical order.
pub fn letter_counts(text: &str) -> Vec<(char, usize)> {
    let mut counts: HashMap<char, usize> = HashMap::new();
    let letters = text
        .chars()
        .flat_map(char::to_lowercase)
        .filter(|c| c.is_alphabetic());
    for letter in letters {
        *counts.entry(letter).or_insert(0) += 1;
    }
    let mut counts: Vec<(char, usize)> = counts.into_iter().collect();
    counts.sort_by(|a, b| b.1.cmp(&a.1).then(a.0.cmp(&b.0)));
    counts
}

/// The distinct words of `text`, lower-cased and without surrounding punctuation, sorted.
pub fn unique_words(text: &str) -> Vec<String> {
    text.split_whitespace()
        .map(|piece| piece.trim_matches(|c: char| !c.is_alphanumeric()))
        .filter(|word| !word.is_empty())
        .map(str::to_lowercase)
        .collect::<BTreeSet<String>>()
        .into_iter()
        .collect()
}
