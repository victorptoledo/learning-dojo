# Control Flow Workout

Implement the four functions in `src/lib.rs`.

1. `fizzbuzz(n)` returns the values from 1 to `n`: `"Fizz"` for multiples of 3,
   `"Buzz"` for multiples of 5, `"FizzBuzz"` for both, the number otherwise.
   `fizzbuzz(0)` is empty.
2. `collatz_steps(n)` counts the steps to reach 1: halve even numbers,
   turn odd ones into `3n + 1`. `collatz_steps(1)` is `0`. `n` is at least 1.
3. `classify(n)` returns `"negative"`, `"zero"`, `"small"` (1 to 9) or `"large"` (10 and up).
   Use a single `match`.
4. `longest_word(text)` returns the longest whitespace-separated word, borrowed from `text`.
   On a tie the first one wins; an empty or blank text returns `None`.
