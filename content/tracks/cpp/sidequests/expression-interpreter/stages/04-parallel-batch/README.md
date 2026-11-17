Evaluate many formulas at once, on several threads. Add to `expr.hpp`:

```cpp
// Parses and evaluates every formula with `threads` threads (0: one per hardware thread); the
// results are in the order of the formulas.
std::vector<std::expected<double, Error>> evaluate_all(std::span<const std::string> formulas,
                                                       const Environment& env, unsigned threads);
```

- Result `i` is what `parse(formulas[i])` and then `evaluate` give, error or value, whatever
  the number of threads.
- Start at most `threads` threads, and never more than there are formulas. `0` means
  `std::thread::hardware_concurrency()` (at least 1).
- Share the work, not a fixed slice per thread: a formula can be much slower than another (an
  environment function may take its time), so a thread that is done takes the next formula
  still waiting. One `std::atomic<std::size_t>` counter is enough to hand out indices.
- `std::jthread` joins in its destructor: a vector of them going out of scope waits for every
  thread, even if something throws (RAII again).

From this stage on the dojo runs all the tests under ThreadSanitizer. The environment is read
by every thread at once, which is safe only because nobody writes to it; each result slot is
written by one thread only. A data race, even one that never gives a wrong number, fails the
check.
