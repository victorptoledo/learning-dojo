use std::collections::HashMap;
use std::ops::Range;

/// Word counts of one text, on the current thread.
pub fn count_words(text: &str) -> HashMap<String, usize> {
    todo!("count the words of {text:?}")
}

/// Adds every count of `from` into `into`.
pub fn merge(into: &mut HashMap<String, usize>, from: HashMap<String, usize>) {
    todo!("merge {} counts into {}", from.len(), into.len())
}

/// Splits `0..len` into at most `workers` balanced, non-empty ranges.
pub fn chunk_ranges(len: usize, workers: usize) -> Vec<Range<usize>> {
    todo!("split {len} items for {workers} workers")
}

/// Counts the words of all `texts`, one scoped thread per chunk.
pub fn parallel_count(texts: &[&str], workers: usize) -> HashMap<String, usize> {
    todo!("count {} texts with {workers} workers", texts.len())
}
