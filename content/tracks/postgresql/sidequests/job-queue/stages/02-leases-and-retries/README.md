Workers crash, get redeployed and lose their network. A job claimed by a worker that died
must not stay `running` forever, and a job that keeps failing must not be retried forever
either. Change `queue.sql`.

**New columns**

| Column | Rule |
|---|---|
| `max_attempts` | `integer`, required, `5` by default |
| `last_error` | `text`: the error of the last failed attempt |

**Leases.** A claim lasts 30 seconds (`locked_until`). After that, the job is up for grabs:

- `claim` also takes a `running` job whose lease has expired (`locked_until < now()`), as
  long as it still has attempts left (`attempts < max_attempts`). It competes with the
  pending jobs in the same order (priority, `run_at`, `id`), and claiming it works as before:
  a new worker, a new lease, one more attempt.
- A `running` job whose lease expired **and** that used all its attempts is never claimed
  again: the next `claim` of its queue marks it `dead`.
- `heartbeat(p_job bigint, p_worker text) RETURNS boolean`: a worker still busy with a job
  extends its lease to `now() + 30 seconds` and gets `true`. If the job is no longer
  `running` for that worker (another worker took it after the lease expired, or it is done),
  it changes nothing and returns `false`: the worker must stop.

**Failures.** `fail(p_job bigint, p_worker text, p_error text) RETURNS job_status`, called by
the worker holding the job (`running`, `locked_by = p_worker`), records `p_error` in
`last_error`, releases the job (`locked_by` and `locked_until` become `NULL`) and returns its
new status:

- with attempts left, it is `pending` again, with `run_at = now() + 10 s × 2^(attempts − 1)`:
  10 s after the first attempt, 20 s after the second, 40 s after the third...
- after its last attempt it is `dead`.

Called by any other worker, or for a job that is not running, `fail` changes nothing and
returns `NULL`. `complete` and the races of stage 1 keep working.

> [!TIP]
> The tests cannot wait 30 seconds, so they move time the other way: they set `locked_until`
> or `run_at` in the past themselves. Within one transaction `now()` never moves, so
> `now() + interval '20 seconds'` is an exact value the tests can compare. Keep your index in
> step with the new `WHERE` of `claim`: the 200,000 done jobs are still there.
