# Parallel Word Count

Count the words of many texts using several threads.

A **word** follows the same rules as the Word Counter kata: split on whitespace, trim the
punctuation at both ends, lowercase, and drop pieces that become empty.

Implement in `src/lib.rs`:

1. `count_words(text) -> HashMap<String, usize>`: the counts of one text, single-threaded.
2. `merge(into, from)`: add every count of `from` into `into`.
3. `chunk_ranges(len, workers) -> Vec<Range<usize>>`: split the indexes `0..len` into at most
   `workers` contiguous, non-empty ranges whose sizes differ by at most one, the bigger ones
   first. `workers == 0` counts as 1; `len == 0` gives no ranges.

   ```rust
   chunk_ranges(10, 3) == vec![0..4, 4..7, 7..10]
   chunk_ranges(2, 5)  == vec![0..1, 1..2]
   ```
4. `parallel_count(texts, workers) -> HashMap<String, usize>`: start **one thread per range**
   with `std::thread::scope`, count each range of texts in its thread, and merge the results.
   The result must equal counting everything on one thread.

> Tip: inside `thread::scope(|s| { ... })`, `s.spawn(|| ...)` returns a handle whose `join()`
> gives the closure's result. The closure can borrow `texts` directly.
