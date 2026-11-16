# Match and Borrow

Practice for *Control Flow Workout*. Implement the three functions in `src/lib.rs`.

1. `grade(score)` turns a score out of 100 into a letter with a single `match`:
   90 to 100 is `'A'`, 80 to 89 `'B'`, 70 to 79 `'C'`, 60 to 69 `'D'`, 0 to 59 `'F'`.
   A score above 100 gives `None`.
2. `sum_until_negative(values)` adds the values up to, but not including, the first
   negative one. An empty slice sums to `0`.
3. `first_word_at_least(text, min_len)` returns the first whitespace-separated word with
   at least `min_len` characters, **borrowed from `text`** (no copy). `None` when there is
   no such word.

Save a file to run the checks. Passing gives practice XP; it does not change the kata.
