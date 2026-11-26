# Countdown, block_on and a Port Parser

Practice for the *Final Exam*: the two halves of it, in small.

1. **A future by hand.** `countdown(steps)` returns a `Countdown` that is `Pending` for `steps`
   polls and then `Ready` with the number of times it was polled (`countdown(2)` gives 3). Each
   time it returns `Pending` it must **wake** the waker of that poll
   (`cx.waker().wake_by_ref()`), or nobody will poll it again.
2. **An executor.** `block_on(future)` polls a future on the current thread and **parks** the
   thread while it is pending; its waker unparks the thread.
3. **A typed error with a cause.** `parse_port(text)` accepts a whole number from 1024 to 65535,
   with spaces around it.
   - `NotANumber { value, source }`: `` `http` is not a port ``; `value` is the trimmed text and
     `source` the `ParseIntError`, returned by `Error::source`.
   - `Reserved { port }`: `port 80 is reserved (below 1024)`, with no source.
4. `chain(error)` joins an error and its causes with `": "`.

Save a file to run the checks. Passing gives practice XP; it does not change the kata.
