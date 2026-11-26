# Meters, max_of! and TinyStack

Practice for *Checkpoint 4*: one small piece for each lesson of the phase.

1. **A newtype with operators.** `Meters(f64)` supports `a + b`, `a - b`, `-a` and `a * 2.0`
   (`Mul<f64>`), and prints as `3.5 m` with `Display`.
2. **A recursive macro.** `max_of!(a, b, c, ...)` returns the largest of one or more expressions,
   comparing with `>` (the first one wins a tie). Each expression is evaluated **once**, and a
   trailing comma is accepted.
3. **A tiny unsafe stack.** `TinyStack<T, N>` keeps up to `N` values in
   `[MaybeUninit<T>; N]`: `push` gives the value back in `Err` when full, `pop` moves the top
   value out, `peek` borrows it, `len` counts, and `Drop` drops what is left, each value exactly
   once.

Save a file to run the checks. Passing gives practice XP; it does not change the kata.
