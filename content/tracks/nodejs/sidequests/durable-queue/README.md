# Sidequest: Durable job queue

Background jobs (send the e-mails, resize the pictures, build the report) must not vanish
when the process dies, must not freeze the server while they run, and must not run twice by
accident. Build a small version of what BullMQ, Sidekiq or a database-backed queue gives you,
with nothing but Node.js: a queue kept in an append-only journal on disk, and a runner that
executes the jobs on a pool of worker threads.

```ts
const queue = await openQueue("data/queue");
await queue.add("thumbnail", { photo: "beach.jpg" }, { priority: 5, maxAttempts: 3 });

const runner = createRunner(queue, { handlers: new URL("./handlers.ts", import.meta.url), concurrency: 4 });
runner.start();
process.once("SIGTERM", async () => {
  await runner.stop({ timeoutMs: 10_000 }); // what is still running goes back to the queue
  await queue.close();
});
```

`queue.ts` holds the queue and the runner, and `worker.ts` (from stage 2) is the worker
thread's entry point. The contract tests open queues in temporary folders, reopen them as a
restarted process would, kill a child process with `SIGKILL` in the middle of its work, tear
the journal's last line, and run CPU-bound, failing, crashing and never-ending handlers from
`tests/handlers.ts` on your workers. The exported names are fixed; the journal's line format
and everything else is yours. Only Node.js itself. `tests/` is yours too.
