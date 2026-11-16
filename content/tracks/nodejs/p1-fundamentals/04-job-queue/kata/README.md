# Job Queue

Three small pieces of event-driven Node.js, plus one prediction.

## 1. predict.ts

Read `demo.ts` (do not run it yet). It schedules six callbacks from inside an I/O callback:
`setTimeout`, `setImmediate`, `process.nextTick`, a promise and `queueMicrotask`. Write the
letters in the order they are logged into `predictedOrder`. The test runs the demo and
compares, so you will know at once if your model of the event loop is right.

## 2. JobQueue in queue.ts

`new JobQueue(worker, concurrency = 1)` runs jobs with an async `worker`, never more than
`concurrency` at a time, in the order they were pushed. It is an `EventEmitter` whose events are
typed by `QueueEvents`:

| Event | Arguments | When |
| --- | --- | --- |
| `start` | `id` | A job starts running |
| `done` | `id, result` | Its promise resolved |
| `failed` | `id, error` | Its promise rejected. A thrown value that is not an `Error` becomes `new Error(String(value))` |
| `drain` | none | The last job finished and nothing is waiting |

| Member | Does |
| --- | --- |
| constructor | Throws a `RangeError` when `concurrency` is not a whole number of at least 1 |
| `push(job)` | Adds the job and returns its id: 1, 2, 3 ... It must **not** start the job synchronously: a listener added right after `push()` still sees its `start` |
| `running` / `waiting` | How many jobs are running / pushed but not started |
| `idle()` | Resolves when nothing is running or waiting (at once if the queue is already idle) |

A failing job must never emit `"error"`: on an `EventEmitter`, an `"error"` event with no
listener throws and crashes the process.

## 3. Helpers in queue.ts

| Function | Does |
| --- | --- |
| `waitFor(emitter, event, ms)` | Resolves with the arguments of the next `event`. Rejects with the error if the emitter emits `"error"` first, and with `new Error('timed out waiting for "<event>" after <ms> ms')` after `ms`. Leaves no listener behind |
| `processInChunks(items, fn, chunkSize)` | Calls `fn(item, index)` for every item, `chunkSize` items at a time, and **lets the event loop turn** between chunks so timers, I/O and `setImmediate` callbacks can run. Resolves with the number of chunks; `RangeError` for a chunk size below 1 |

A promise that is already resolved does not let the loop turn: microtasks run before the loop
moves on. You need a macrotask, such as `setImmediate` from `node:timers/promises`.

Run the tests with `npm test` after `npm install`. There are 14 tests: 1 in
`tests/visible.test.ts` and 13 hidden ones.
