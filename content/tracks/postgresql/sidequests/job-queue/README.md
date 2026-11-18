# Sidequest: Job queue

Sending e-mails, resizing images, calling a slow partner API: a web app hands that work to
background workers through a queue. Many teams already run PostgreSQL and would rather not
run a message broker as well, and PostgreSQL can be that queue, if the workers never step on
each other. Ten workers asking for the next job at the same moment must get ten different
jobs, at once, and a worker that dies must not take its job with it. You build the queue as a
table and PL/pgSQL functions, in four stages:

1. enqueue and claim jobs by priority, with workers that never wait for each other;
2. leases that expire, retries with exponential backoff, and dead jobs;
3. enqueueing a job only once, even when two requests try at the same moment;
4. jobs of the same group (one customer, one account) never running at the same time.

```text
jobs (id, queue, payload, priority, run_at, status, attempts, locked_by, locked_until, ...)
```

How it is checked:

- There is no fixture: everything lives in **one file, `queue.sql`**, which every stage
  changes. Each stage's statement gives the columns and functions the tests use; how you
  write them is yours, and you may add columns, indexes and helper functions.
- The dojo loads `queue.sql` once and **commits** it, then runs two kinds of tests:
  - ordinary tests call your functions inside a transaction that is rolled back; they may
    move time by editing `run_at` or `locked_until` themselves;
  - **race tests** open two real sessions (with `dblink`), start a transaction in one, call
    your function in the other while the first is still open, and check who waited, what each
    one got and what the table holds after both finish.
- So the file must load in one go on an empty database: no `BEGIN`/`COMMIT`, no `psql`
  commands.
- Try things with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play` from
  this folder. Open a second terminal with `docker exec -it <container> psql -d dojo` to be
  the second worker: `BEGIN; SELECT claim('mail', 'w1');` in one, the same in the other.

When you are done, `[A]` copies a review prompt: ask an AI how a worker could still lose a
job or run one twice.
