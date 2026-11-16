# Parallel Sum

Practice for *Parallel Word Count*. Implement the three functions in `src/lib.rs`.

1. `chunk_bounds(len, parts)` splits `0..len` into at most `parts` contiguous, non-empty
   ranges whose sizes differ by at most one, the larger ones first. `parts` of `0` counts
   as `1`; a `len` of `0` gives no ranges. `(10, 3)` → `[0..4, 4..7, 7..10]`.
2. `parallel_sum(values, threads)` adds the values with one scoped thread per chunk
   (`std::thread::scope`), borrowing the slice instead of copying it.
3. `parallel_longest(words, threads)` is the length, in characters, of the longest word,
   computed the same way. `None` when there are no words.

Save a file to run the checks. Passing gives practice XP; it does not change the kata.
