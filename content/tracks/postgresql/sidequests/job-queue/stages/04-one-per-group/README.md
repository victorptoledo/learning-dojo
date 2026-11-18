Some jobs must not run side by side: two syncs of the same customer's account would overwrite
each other, and a partner API allows one call per customer at a time. Jobs now may belong to
a **group**, and a group runs one job at a time while the other groups go on. Change
`queue.sql`.

**New column:** `group_key text`, `NULL` for jobs without a group.

**The function.** `enqueue_grouped(p_queue text, p_group text, p_payload jsonb) RETURNS
bigint` enqueues a job of that group (priority 0, `run_at = now()`) and returns its id.

**The rule.** `claim` never returns a job whose group already has a job **running with a live
lease** (`locked_until >= now()`) in that queue: it skips the whole group and takes the next
job in line. Once that job is done, failed or its lease expired, the group's next job can run.
Jobs without a group are not affected, and the rules of the earlier stages still hold.

**The race.** Jobs 1 and 2 belong to group `acme`, job 3 to `globex`. Worker A claims job 1
inside a transaction that stays open; worker B claims. B **does not wait**, and it gets job 3,
not job 2: A's claim is not committed, so B cannot see that `acme` is running, and yet it
must not run job 2. If A then rolls back, B's next claim gets job 1.

> [!TIP]
> Row locks cannot help here: B must stay away from job 2, a row nobody has locked. An
> **advisory lock** is a lock on a number you choose. `pg_try_advisory_xact_lock(key)` takes
> it or returns `false` at once if another transaction holds it, and releases it when the
> transaction ends. A key per group, such as `hashtext(queue || ':' || group_key)`, makes "is
> someone claiming in this group right now?" a question B can ask. After A commits, the
> `running` row answers it. Loop over the candidates in order and take the first one that
> passes both checks and that `FOR UPDATE SKIP LOCKED` can lock.
