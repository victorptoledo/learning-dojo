# Borrowed Fields and a Background Count

Practice for *Checkpoint 3*. Implement the three functions in `src/lib.rs`.

1. `fields(line)` splits a line on `,`, trims each piece and drops the empty ones. The fields
   are **slices of `line`**: no `String`. `" a, b ,,c "` → `["a", "b", "c"]`.
2. `longer(a, b)` returns the longer of two string slices (in characters), `a` on a tie. Its
   signature says the result lives as long as both inputs.
3. `count_matching(items, workers, test)` counts the items for which `test` returns `true`, on
   `workers` threads made with `std::thread::spawn` (`0` counts as 1). The threads cannot borrow:
   give each one its own part of the owned `items` and a clone of the `Arc`, and add up the
   counts in an `Arc<AtomicUsize>` or through `join`. The items must be tested in parallel.

Save a file to run the checks. Passing gives practice XP; it does not change the kata.
