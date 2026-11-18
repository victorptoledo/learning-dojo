A customer double-clicks "Resend invoice", a webhook arrives twice, a cron job overlaps with
its previous run: the same job gets enqueued two or three times. Callers will now give a job
a **key**, and the queue keeps at most one live job per key. Change `queue.sql`.

**New column:** `dedupe_key text`, `NULL` for jobs without a key.

**The rule, in the table.** A queue never has two jobs with the same `dedupe_key` that are
both **live** (`pending` or `running`): a second one fails with a unique violation (`23505`),
whatever code inserts it. Jobs that are `done` or `dead` do not count, so the same key can
run again later, and jobs without a key are never compared.

**The function.** `enqueue_once(p_queue text, p_key text, p_payload jsonb) RETURNS bigint`:

- when the queue has a live job with that key, it returns that job's id and changes nothing
  (the payload of the first one wins);
- otherwise it enqueues a new job (priority 0, `run_at = now()`) with that key and returns
  its id.

**The races**

- Session A calls `enqueue_once('mail', 'invoice-42', ...)` inside a transaction that stays
  open; session B makes the same call. B **waits** for A. If A commits, B returns A's job;
  if A rolls back, B enqueues the job itself. Either way, one job, and never an error.
- Different keys never wait for each other.

> [!TIP]
> A **partial** unique index (`CREATE UNIQUE INDEX ... WHERE ...`) enforces uniqueness only
> among the rows that match its condition. A second insert of a key another transaction has
> just inserted waits for that transaction to end, then fails or goes ahead. `INSERT ... ON
> CONFLICT DO NOTHING` turns the failure into "no row": to target a partial index, repeat its
> columns and its condition, `ON CONFLICT (queue, dedupe_key) WHERE ...`. Then, in a new
> statement, read the job that won.
