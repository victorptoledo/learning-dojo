use std::collections::HashMap;
use std::ops::Range;
use std::thread;

/// Word counts of one text, on the current thread.
pub fn count_words(text: &str) -> HashMap<String, usize> {
    let mut counts = HashMap::new();
    let words = text
        .split_whitespace()
        .map(|piece| piece.trim_matches(|c: char| !c.is_alphanumeric()))
        .filter(|word| !word.is_empty());
    for word in words {
        *counts.entry(word.to_lowercase()).or_insert(0) += 1;
    }
    counts
}

/// Adds every count of `from` into `into`.
pub fn merge(into: &mut HashMap<String, usize>, from: HashMap<String, usize>) {
    for (word, count) in from {
        *into.entry(word).or_insert(0) += count;
    }
}

/// Splits `0..len` into at most `workers` balanced, non-empty ranges.
pub fn chunk_ranges(len: usize, workers: usize) -> Vec<Range<usize>> {
    let chunks = len.min(workers.max(1));
    if chunks == 0 {
        return Vec::new();
    }
    let (base, extra) = (len / chunks, len % chunks);
    let mut start = 0;
    (0..chunks)
        .map(|index| {
            let size = base + usize::from(index < extra);
            let range = start..start + size;
            start += size;
            range
        })
        .collect()
}

/// Counts the words of all `texts`, one scoped thread per chunk.
pub fn parallel_count(texts: &[&str], workers: usize) -> HashMap<String, usize> {
    thread::scope(|scope| {
        let handles: Vec<_> = chunk_ranges(texts.len(), workers)
            .into_iter()
            .map(|range| {
                let chunk = &texts[range];
                scope.spawn(move || {
                    let mut counts = HashMap::new();
                    for text in chunk {
                        merge(&mut counts, count_words(text));
                    }
                    counts
                })
            })
            .collect();
        let mut total = HashMap::new();
        for handle in handles {
            merge(&mut total, handle.join().expect("a counting thread panicked"));
        }
        total
    })
}
