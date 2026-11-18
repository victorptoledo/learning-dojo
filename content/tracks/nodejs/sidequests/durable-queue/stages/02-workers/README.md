Now run the jobs, on worker threads, so a CPU-heavy job never blocks the event loop. Add to
`queue.ts`:

```ts
export interface RunnerOptions {
  handlers: URL | string;   // a module whose default export maps job types to functions
  concurrency?: number;     // default 2
  timeoutMs?: number;       // default 30000
}
export interface Runner {
  start(): void;
  drain(): Promise<void>;   // resolves when no job is queued or running
  stop(): Promise<void>;    // lets the running jobs finish, takes nothing new, ends the workers
}
export function createRunner(queue: Queue, options: RunnerOptions): Runner;
```

and write `worker.ts`, the worker thread's entry point
(`new Worker(new URL("./worker.ts", import.meta.url), { workerData })`).

**The handlers.** The module named by `handlers` exports by default an object such as
`{ square: (payload) => payload.n ** 2 }`. A handler gets `(payload, { id, attempt })`
(`attempt` is the job's `attempts`) and returns a value or a promise. Each worker imports the
module once (`await import(url)`) and then runs one job per message, posting the outcome back
to the main thread: the result, or the error's message when the handler throws or rejects. An
unknown type is an error that names the type.

**The runner.**

- `start()` creates `concurrency` workers and keeps them: a job goes to an idle worker, and a
  worker runs one job at a time. Creating a thread per job would cost more than most jobs.
- Each idle worker `take()`s the next job. When a job ends, the runner calls
  `queue.complete(id, result)` or `queue.fail(id, message)` and takes the next one.
- Jobs added after `start()` run without polling: listen to the queue's `"added"` event.
- A worker that exits while running a job (a handler calling `process.exit(3)`, a crash) fails
  the job with a message that has `exit` and the exit code in it, and is replaced by a new
  worker.
- A job still running after `timeoutMs` fails with a message containing `timed out`: a
  handler stuck in a loop cannot be interrupted, so terminate its worker
  (`worker.terminate()`) and start a new one.
- `drain()` resolves once nothing is queued or running, and the results are saved.
- `stop()` takes no new job, waits for the running ones, then terminates every worker, so the
  process can exit.

The tests measure it: six 250 ms CPU-bound jobs on two workers overlap two at a time and take
far less than 1.5 s, a timer on the main thread keeps ticking meanwhile, and twelve jobs on
three workers run on at most three threads.
