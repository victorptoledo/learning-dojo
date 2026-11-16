# Task Queue

## Part 1: `BlockingQueue<T>` (in `queue.hpp`)

| Member | Does |
| --- | --- |
| `BlockingQueue(capacity = 0)` | 0 means unbounded. |
| `push(value)` | Adds at the back. Waits while the queue is full. Returns `false` once the queue is closed, also when it closes while `push` waits. |
| `pop()` | Waits for a value and removes it. Returns `std::nullopt` only when the queue is closed **and** empty: values pushed before `close()` are still handed out. |
| `try_pop()` | Like `pop`, without waiting. |
| `close()` | No more pushes; wakes every waiting thread. Calling it twice is fine. |
| `closed()`, `size()` | Read under the lock. |

## Part 2: `WorkerPool` (in `pool.cpp`)

The constructor starts `threads` workers (at least one) on `std::jthread`. Each worker pops
tasks and runs them until the queue is closed and empty. A task that throws is counted in
`failed()`, and the worker goes on. `shutdown()` closes the queue, lets the workers finish
what is queued and joins them; the destructor calls it. `async(f)` is already written: read
how it wraps a `std::packaged_task` so the future gets the result or the exception.

## Part 3: `parallel_sum(values, threads)`

Split the vector into `threads` chunks (0 counts as 1, never more chunks than values), add
each chunk on its own thread into its own slot of a vector of partial sums, join, and add
the partial sums. No mutex is needed because no two threads write the same memory.

## ThreadSanitizer

This kata runs under **ThreadSanitizer** instead of AddressSanitizer. Reading `size()` or
`closed_` without the lock, or two workers bumping a plain `int`, fails the test with
`ThreadSanitizer: data race at file:line`. A deadlock or a thread that never finishes fails
with a timeout after 10 seconds.

16 hidden tests and the visible one.
