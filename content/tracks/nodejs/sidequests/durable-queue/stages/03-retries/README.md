Most failures are temporary: a timeout, a server restarting. Try again, but not at once and
not forever.

- `Job` gains `maxAttempts`, `backoffMs` and `runAt` (ms since the epoch), and `AddOptions`
  gains `maxAttempts` (default `1`) and `backoffMs` (default `100`). A new job's `runAt` is its
  `createdAt`.
- **`fail(id, error)`** keeps the error, then: while `attempts < maxAttempts`, the job is
  `queued` again with `runAt = Date.now() + backoffMs * 2 ** (attempts - 1)` (100, 200,
  400 ms... with the default); otherwise it is `failed` for good. The failed jobs are the
  queue's dead letters, `list("failed")`.
- **`take()`** only hands out jobs whose `runAt` has come, so a job waiting for its retry
  does not block a ready job behind it, whatever its priority.
- **`retry(id)`** puts a `failed` job back: `queued`, `attempts` 0, `runAt` now; it rejects for
  any other state. It emits `"retried"` with the job.
- Everything is in the journal: a retry waiting when the process restarts keeps its `runAt`.

**The runner** keeps working through the retries: a crash and a timeout are failures like any
other, so they are retried too. When no job is ready but some are waiting, set a timer for the
earliest `runAt` instead of polling. Run retried jobs (`"retried"`) like new ones. `drain()`
now also waits for queued jobs whose `runAt` is still ahead.
