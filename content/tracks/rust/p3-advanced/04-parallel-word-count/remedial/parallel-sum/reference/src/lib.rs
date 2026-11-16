use std::ops::Range;
use std::thread;

/// `0..len` in at most `parts` balanced, contiguous ranges, the larger ones first.
pub fn chunk_bounds(len: usize, parts: usize) -> Vec<Range<usize>> {
    let chunks = len.min(parts.max(1));
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

/// Sum of `values`, one scoped thread per chunk.
pub fn parallel_sum(values: &[u64], threads: usize) -> u64 {
    thread::scope(|scope| {
        let workers: Vec<_> = chunk_bounds(values.len(), threads)
            .into_iter()
            .map(|range| {
                let chunk = &values[range];
                scope.spawn(move || chunk.iter().sum::<u64>())
            })
            .collect();
        workers
            .into_iter()
            .map(|worker| worker.join().expect("a worker panicked"))
            .sum()
    })
}

/// Length in characters of the longest word, one scoped thread per chunk.
pub fn parallel_longest(words: &[&str], threads: usize) -> Option<usize> {
    thread::scope(|scope| {
        let workers: Vec<_> = chunk_bounds(words.len(), threads)
            .into_iter()
            .map(|range| {
                let chunk = &words[range];
                scope.spawn(move || chunk.iter().map(|word| word.chars().count()).max())
            })
            .collect();
        workers
            .into_iter()
            .filter_map(|worker| worker.join().expect("a worker panicked"))
            .max()
    })
}
