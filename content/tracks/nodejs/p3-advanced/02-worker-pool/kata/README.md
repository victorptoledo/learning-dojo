# Worker Pool

CPU-bound work, such as hashing, image resizing or parsing a huge document, blocks the event
loop: while it runs, no request is answered. Worker threads run JavaScript on other threads.
Starting a worker costs milliseconds and memory, so real programs keep a **pool** of them (as
Piscina does). Write one.

`protocol.ts` defines the messages and is given. Write two files.

## worker.ts

The code that runs inside each worker thread. Export `countPrimes(below)`, the number of primes
smaller than `below` (a sieve of Eratosthenes is fast enough), and listen on `parentPort` for
`Request` messages. Answer each one with a `Reply` carrying the same `id`:

| Task | Answer |
| --- | --- |
| `{ kind: "countPrimes", below }` | `{ id, ok: true, value: countPrimes(below) }` |
| `{ kind: "spin", ms }` | Keep the CPU busy for `ms` milliseconds (a `while` loop on `performance.now()`), then `{ id, ok: true, value: ms }` |
| `{ kind: "fail", message }` | `{ id, ok: false, error: message }`: an error is a message, not a crash |
| `{ kind: "crash", code }` | `process.exit(code)`. In a worker this ends only that thread |

The tests also import `countPrimes` on the main thread, where `parentPort` is `null`: use
`parentPort?.on(...)`.

## pool.ts

| Member | Does |
| --- | --- |
| `new WorkerPool(size?, file?)` | Starts `size` workers (default `availableParallelism()`) from `file` (default `WORKER_FILE`). `RangeError` when `size` is not a whole number of at least 1 |
| `run(task)` | Resolves with the worker's `value`, or rejects with `new Error(error)` for an `ok: false` reply. Each worker runs **one task at a time**; extra tasks wait in a queue, first in first out |
| `stats()` | `{ size, busy, queued }`: workers, workers running a task, tasks waiting |
| `close()` | Rejects every queued and running task with `new Error("pool closed")`, terminates every worker and resolves when they are gone. After it, `run` rejects with the same message |

## When a worker dies

A worker that exits (the `"exit"` event) or throws (`"error"`) while running a task must:

1. reject that task with `new Error("worker exited with code <code>")` (or the error),
2. be **replaced** by a new worker, so the pool keeps its size,
3. not stop the queue: the next tasks run as usual.

Keep the pool's bookkeeping straight: a `Map` or an array of `{ worker, job }` slots, and an
`id` on every request so a reply can never be matched to the wrong task.

Run the tests with `npm test` after `npm install`. Node.js runs the worker's `.ts` file
directly, so `new Worker(new URL("./worker.ts", import.meta.url))` just works. There are 10
tests: 1 in `tests/visible.test.ts` and 9 hidden ones.
