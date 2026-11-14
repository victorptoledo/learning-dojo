# Parallel Fetch

A page needs data from many slow services. Calling them one after the other takes the sum
of their times; calling them **at the same time** takes about as long as the slowest one.

`Outcome<T>` (already written) is a sealed interface: `Success(value)`, `Failure(message)` or
`TimedOut()`. Implement `ParallelFetch`:

1. `fetchAll(calls, timeout)`: run every `Callable` on **its own virtual thread** and return
   one `Outcome` per call, **in the order of the calls**.
   - The `timeout` is **one deadline for the whole batch**, counted from when `fetchAll`
     starts: a call that has not answered by then is `TimedOut`, and must be **cancelled**
     (interrupted) so `fetchAll` returns right away instead of waiting for it.
   - A call that throws is `Failure` with the exception's message, or its class's simple name
     when the message is `null`.
2. `successes(outcomes)`: the values of the `Success` outcomes, in order.
3. `summary(outcomes)`: `"3 ok, 1 failed, 1 timed out"`. Use a `switch` over the sealed type.

> Tip: `Executors.newVirtualThreadPerTaskExecutor()` in a try-with-resources block,
> `executor.submit(call)` for each call, `future.get(left, TimeUnit.NANOSECONDS)` with
> `left = deadline - System.nanoTime()`, `future.cancel(true)`, and `ExecutionException.getCause()`.
> Closing the executor waits for every task, so cancel the stuck ones before you leave the block.
