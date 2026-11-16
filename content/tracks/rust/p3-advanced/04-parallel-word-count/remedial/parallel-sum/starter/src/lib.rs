use std::ops::Range;

/// `0..len` in at most `parts` balanced, contiguous ranges, the larger ones first.
pub fn chunk_bounds(len: usize, parts: usize) -> Vec<Range<usize>> {
    todo!("split 0..{len} into {parts} parts")
}

/// Sum of `values`, one scoped thread per chunk.
pub fn parallel_sum(values: &[u64], threads: usize) -> u64 {
    todo!("sum {} values on {threads} threads", values.len())
}

/// Length in characters of the longest word, one scoped thread per chunk.
pub fn parallel_longest(words: &[&str], threads: usize) -> Option<usize> {
    todo!("longest of {} words on {threads} threads", words.len())
}
