The last two things a real queue needs: cancelling a job, and a shutdown that neither waits
forever nor loses work.

- `JobState` gains `"cancelled"`.
- **`cancel(id)`**: a `queued` or `running` job becomes `cancelled`, is never taken again and
  resolves `true`; a job that already finished (or was cancelled) resolves `false`; an unknown
  id rejects. It emits `"cancelled"` with the job.
- **`release(id)`**: a `running` job goes back to `queued`, `runAt` now, and the attempt does
  not count (`attempts` goes down by one). It rejects for any other state.

**The runner:**

- When a job it is running gets cancelled, the runner terminates that worker at once (a
  handler stuck in a loop never looks at messages), starts a new one and takes the next job.
  A cancelled job stays `cancelled`: it is not failed or retried, and an answer that arrives
  after the cancel is ignored.
- **`stop({ timeoutMs })`**: takes no new job and waits for the running ones, but only until
  the deadline. Jobs still running then are `release`d (back to the queue, the attempt not
  counted) and their workers terminated, so the next process starts them again. `stop()`
  resolves once every worker has ended and every change is written, and returns as soon as
  the work is done when that comes before the deadline. Without `timeoutMs` it waits as long
  as it takes, as before.

After `stop`, a new runner on the same queue picks up where the old one left off.
