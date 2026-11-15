# Job Queue

`fixture/01_jobs.sql` creates one table:

```text
jobs (id, queue, payload jsonb, status: ready | running | done | dead, priority,
      run_at, attempts, max_attempts, locked_by, locked_at, last_error)
```

A job is **due** when it is `ready` and its `run_at` is not in the future. Several workers run
the same statements at the same time. "Now" is `TIMESTAMPTZ '2026-11-15 12:00+00'` in every
file (a real worker would use `now()`).

Write one statement in each file:

| File | Does | Returns |
| --- | --- | --- |
| `claim.sql` | `worker-1` takes the next due job of queue `emails`: highest `priority` first, then oldest `run_at`, then lowest `id`. It sets `status = 'running'`, `locked_by = 'worker-1'`, `locked_at` = now and adds 1 to `attempts`. A job that another worker has locked at this moment must be **skipped, not waited for** | `id`, `attempts` of the claimed job (no row when nothing is due) |
| `fail.sql` | job 7 (running) failed with the error `smtp timeout`. If its `attempts` reached `max_attempts`, it becomes `dead` and keeps its `run_at`; otherwise it becomes `ready` again with `run_at` = now + 30 seconds × 2^attempts (60 s after the first attempt, 120 s after the second). Either way it is unlocked (`locked_by`, `locked_at` NULL) and `last_error` is set | `id`, `status`, `run_at` |
| `reap.sql` | running jobs whose `locked_at` is more than 5 minutes before now belong to a worker that crashed: put them back to `ready` and unlock them | `id` of each released job |
| `queue_stats.sql` | a report per queue: due jobs, scheduled jobs (ready, `run_at` in the future), running and dead jobs (done jobs are not counted) | `queue`, `due`, `scheduled`, `running`, `dead`, by queue |

Why one statement for the claim? A `SELECT` followed by an `UPDATE` lets two workers pick the
same job between the two. One hidden test opens a second session that holds a lock on the job
your claim would take, and your claim must return the next one instead, without waiting.

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`. To see the
lock yourself, open two psql sessions in one container: `docker exec -it <container> psql -d dojo`.

There are 12 tests: 1 in `tests/visible.sql` and 11 hidden ones.
