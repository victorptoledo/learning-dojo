// A durable job queue: every change is a line in an append-only journal, replayed on open, and
// a runner that executes the jobs on a pool of worker threads.
import { EventEmitter } from "node:events";
import { mkdir, open, readFile, rename, truncate, type FileHandle } from "node:fs/promises";
import path from "node:path";
import { Worker } from "node:worker_threads";

export type JobState = "queued" | "running" | "done" | "failed" | "cancelled";

export interface Job {
  /** "1", "2", ...: unique and never reused, also across restarts. */
  id: string;
  type: string;
  payload: unknown;
  priority: number;
  state: JobState;
  /** How many times the job was taken. */
  attempts: number;
  maxAttempts: number;
  backoffMs: number;
  /** Not taken before this time (ms since the epoch). */
  runAt: number;
  createdAt: number;
  result?: unknown;
  error?: string;
}

export interface AddOptions {
  priority?: number;
  maxAttempts?: number;
  backoffMs?: number;
}

export const JOURNAL = "journal.ndjson";

export class Queue extends EventEmitter {
  readonly #dir: string;
  readonly #jobs = new Map<string, Job>();
  #nextId = 1;
  #file: FileHandle | undefined;
  #writing: Promise<void> = Promise.resolve();
  #closed = false;

  constructor(dir: string) {
    super();
    this.#dir = dir;
  }

  get #journal(): string {
    return path.join(this.#dir, JOURNAL);
  }

  /** Replays the journal: the last line of each job wins; a torn last line is cut off. */
  async load(): Promise<void> {
    await mkdir(this.#dir, { recursive: true });
    let text = "";
    try {
      text = await readFile(this.#journal, "utf8");
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
    }
    const end = text.lastIndexOf("\n") + 1;
    if (end < text.length) {
      await truncate(this.#journal, Buffer.byteLength(text.slice(0, end)));
    }
    for (const line of text.slice(0, end).split("\n")) {
      if (line.trim() === "") continue;
      let job: Job;
      try {
        job = JSON.parse(line) as Job;
      } catch {
        continue;
      }
      this.#jobs.set(job.id, job);
      this.#nextId = Math.max(this.#nextId, Number(job.id) + 1);
    }
    this.#file = await open(this.#journal, "a");
    for (const job of this.#jobs.values()) {
      if (job.state === "running") {
        // The process died while it ran: run it again.
        job.state = "queued";
        await this.#save(job);
      }
    }
  }

  #save(job: Job): Promise<void> {
    if (this.#closed || this.#file === undefined) {
      return Promise.reject(new Error("the queue is closed"));
    }
    const file = this.#file;
    const line = `${JSON.stringify(job)}\n`;
    // One write at a time, in order: lines never interleave.
    const write = this.#writing.then(() => file.write(line)).then(() => {});
    this.#writing = write.catch(() => {});
    return write;
  }

  #change(id: string, from: readonly JobState[], change: (job: Job) => void): Promise<Job> {
    const job = this.#jobs.get(id);
    if (job === undefined) return Promise.reject(new Error(`no job ${id}`));
    if (!from.includes(job.state)) return Promise.reject(new Error(`job ${id} is ${job.state}`));
    change(job);
    return this.#save(job).then(() => structuredClone(job));
  }

  async add(type: string, payload: unknown, options: AddOptions = {}): Promise<Job> {
    if (this.#closed) throw new Error("the queue is closed");
    const now = Date.now();
    const job: Job = {
      id: String(this.#nextId++),
      type,
      payload: structuredClone(payload),
      priority: options.priority ?? 0,
      state: "queued",
      attempts: 0,
      maxAttempts: options.maxAttempts ?? 1,
      backoffMs: options.backoffMs ?? 100,
      runAt: now,
      createdAt: now,
    };
    this.#jobs.set(job.id, job);
    await this.#save(job);
    this.emit("added", structuredClone(job));
    return structuredClone(job);
  }

  /** The next job that may run now: highest priority, then oldest. It becomes running. */
  async take(): Promise<Job | undefined> {
    if (this.#closed) throw new Error("the queue is closed");
    const now = Date.now();
    let best: Job | undefined;
    for (const job of this.#jobs.values()) {
      if (job.state !== "queued" || job.runAt > now) continue;
      if (best === undefined || job.priority > best.priority || (job.priority === best.priority && Number(job.id) < Number(best.id))) {
        best = job;
      }
    }
    if (best === undefined) return undefined;
    return this.#change(best.id, ["queued"], (job) => {
      job.state = "running";
      job.attempts++;
    });
  }

  complete(id: string, result: unknown): Promise<Job> {
    return this.#change(id, ["running"], (job) => {
      job.state = "done";
      job.result = structuredClone(result);
      delete job.error;
    });
  }

  /** A failed attempt: queued again after a backoff while attempts are left, failed after that. */
  fail(id: string, error: string): Promise<Job> {
    return this.#change(id, ["running"], (job) => {
      job.error = error;
      if (job.attempts < job.maxAttempts) {
        job.state = "queued";
        job.runAt = Date.now() + job.backoffMs * 2 ** (job.attempts - 1);
      } else {
        job.state = "failed";
      }
    });
  }

  /** A failed job runs again from scratch. */
  async retry(id: string): Promise<Job> {
    const job = await this.#change(id, ["failed"], (changed) => {
      changed.state = "queued";
      changed.attempts = 0;
      changed.runAt = Date.now();
    });
    this.emit("retried", job);
    return job;
  }

  /** A running job goes back to the queue, and that attempt does not count. */
  release(id: string): Promise<Job> {
    return this.#change(id, ["running"], (job) => {
      job.state = "queued";
      job.attempts = Math.max(0, job.attempts - 1);
      job.runAt = Date.now();
    });
  }

  /** Cancels a queued or running job; false when it had already finished. */
  async cancel(id: string): Promise<boolean> {
    const job = this.#jobs.get(id);
    if (job === undefined) throw new Error(`no job ${id}`);
    if (job.state !== "queued" && job.state !== "running") return false;
    await this.#change(id, ["queued", "running"], (changed) => {
      changed.state = "cancelled";
    });
    this.emit("cancelled", structuredClone(job));
    return true;
  }

  get(id: string): Job | undefined {
    const job = this.#jobs.get(id);
    return job === undefined ? undefined : structuredClone(job);
  }

  /** Every job (or those in one state), in the order they were added. */
  list(state?: JobState): Job[] {
    return [...this.#jobs.values()]
      .filter((job) => state === undefined || job.state === state)
      .sort((a, b) => Number(a.id) - Number(b.id))
      .map((job) => structuredClone(job));
  }

  /** Rewrites the journal with one line per job, through a temporary file and a rename. */
  async compact(): Promise<void> {
    if (this.#closed || this.#file === undefined) throw new Error("the queue is closed");
    const run = this.#writing.then(async () => {
      const temp = `${this.#journal}.tmp`;
      const lines = this.list().map((job) => `${JSON.stringify(job)}\n`).join("");
      const handle = await open(temp, "w");
      try {
        await handle.write(lines);
        await handle.sync();
      } finally {
        await handle.close();
      }
      await this.#file?.close();
      await rename(temp, this.#journal);
      this.#file = await open(this.#journal, "a");
    });
    this.#writing = run.catch(() => {});
    await run;
  }

  async close(): Promise<void> {
    if (this.#closed) return;
    this.#closed = true;
    await this.#writing;
    await this.#file?.close();
    this.#file = undefined;
  }
}

export async function openQueue(dir: string): Promise<Queue> {
  const queue = new Queue(dir);
  await queue.load();
  return queue;
}

// ---------------------------------------------------------------------------------------------

export interface RunnerOptions {
  /** A module whose default export maps job types to handler functions. */
  handlers: URL | string;
  concurrency?: number;
  timeoutMs?: number;
}

export interface StopOptions {
  /** How long to wait for running jobs; after that they go back to the queue. */
  timeoutMs?: number;
}

export interface Runner {
  start(): void;
  /** Resolves when no job is queued (delayed ones included) or running. */
  drain(): Promise<void>;
  stop(options?: StopOptions): Promise<void>;
}

interface Reply {
  id: string;
  ok: boolean;
  result?: unknown;
  error?: string;
}

interface Slot {
  worker: Worker;
  job: Job | undefined;
  reserved: boolean;
  timer: NodeJS.Timeout | undefined;
  /** Why the runner terminated it, if it did. */
  ending: "timeout" | "cancelled" | "released" | "stopped" | undefined;
  exited: Promise<void>;
}

const WORKER = new URL("./worker.ts", import.meta.url);

export function createRunner(queue: Queue, options: RunnerOptions): Runner {
  const concurrency = options.concurrency ?? 2;
  const timeoutMs = options.timeoutMs ?? 30_000;
  const handlers = typeof options.handlers === "string" ? options.handlers : options.handlers.href;
  const slots: Slot[] = [];
  let started = false;
  let stopping = false;
  let wake: NodeJS.Timeout | undefined;
  let waiting: (() => void)[] = [];
  const pending = new Set<Promise<unknown>>();

  const track = <T>(promise: Promise<T>): Promise<T> => {
    pending.add(promise);
    void promise.finally(() => pending.delete(promise)).catch(() => {});
    return promise;
  };

  const idle = () =>
    slots.every((slot) => slot.job === undefined && !slot.reserved) &&
    queue.list("queued").length === 0 &&
    pending.size === 0;

  const checkDrained = () => {
    if (waiting.length > 0 && idle()) {
      const resolvers = waiting;
      waiting = [];
      for (const resolve of resolvers) resolve();
    }
  };

  function finish(slot: Slot): void {
    clearTimeout(slot.timer);
    slot.timer = undefined;
    slot.job = undefined;
  }

  function spawn(): Slot {
    const worker = new Worker(WORKER, { workerData: { handlers } });
    let exitedResolve: () => void = () => {};
    const slot: Slot = {
      worker,
      job: undefined,
      reserved: false,
      timer: undefined,
      ending: undefined,
      exited: new Promise<void>((resolve) => (exitedResolve = resolve)),
    };
    worker.on("message", (reply: Reply) => {
      const job = slot.job;
      if (job === undefined || job.id !== reply.id || slot.ending !== undefined) return;
      finish(slot);
      const saved = reply.ok ? queue.complete(job.id, reply.result) : queue.fail(job.id, reply.error ?? "failed");
      void track(saved.catch(() => {})).then(() => {
        pump();
        checkDrained();
      });
    });
    worker.on("error", () => {
      // An uncaught error ends the worker; "exit" follows and deals with the job.
    });
    worker.on("exit", (code) => {
      exitedResolve();
      const index = slots.indexOf(slot);
      if (index !== -1) slots.splice(index, 1);
      const job = slot.job;
      const ending = slot.ending;
      finish(slot);
      let saved: Promise<unknown> = Promise.resolve();
      if (job !== undefined) {
        if (ending === "timeout") saved = queue.fail(job.id, `timed out after ${timeoutMs} ms`);
        else if (ending === "released") saved = queue.release(job.id);
        else if (ending === undefined) saved = queue.fail(job.id, `worker exited with code ${code}`);
      }
      void track(saved.catch(() => {})).then(() => {
        if (!stopping && started) slots.push(spawn());
        pump();
        checkDrained();
      });
    });
    return slot;
  }

  function schedule(): void {
    clearTimeout(wake);
    wake = undefined;
    const next = Math.min(...queue.list("queued").map((job) => job.runAt));
    if (Number.isFinite(next)) {
      wake = setTimeout(pump, Math.max(0, next - Date.now()) + 1);
    }
  }

  function pump(): void {
    if (!started || stopping) return;
    for (const slot of slots) {
      if (slot.job !== undefined || slot.reserved || slot.ending !== undefined) continue;
      slot.reserved = true;
      void track(queue.take()).then(
        (job) => {
          slot.reserved = false;
          if (job === undefined) {
            schedule();
            checkDrained();
            return;
          }
          if (stopping) {
            void track(queue.release(job.id).catch(() => {})).then(checkDrained);
            return;
          }
          slot.job = job;
          slot.timer = setTimeout(() => {
            slot.ending = "timeout";
            void slot.worker.terminate();
          }, timeoutMs);
          slot.worker.postMessage({ id: job.id, type: job.type, payload: job.payload, attempt: job.attempts });
        },
        () => {
          slot.reserved = false;
        },
      );
    }
  }

  const onAdded = () => pump();
  const onCancelled = (job: Job) => {
    const slot = slots.find((candidate) => candidate.job?.id === job.id);
    if (slot !== undefined) {
      slot.ending = "cancelled";
      void slot.worker.terminate();
    }
    checkDrained();
  };

  return {
    start() {
      if (started) return;
      started = true;
      for (let i = 0; i < concurrency; i++) slots.push(spawn());
      queue.on("added", onAdded);
      queue.on("retried", onAdded);
      queue.on("cancelled", onCancelled);
      pump();
    },

    drain() {
      return new Promise<void>((resolve) => {
        waiting.push(resolve);
        checkDrained();
      });
    },

    async stop(stopOptions: StopOptions = {}) {
      stopping = true;
      clearTimeout(wake);
      queue.off("added", onAdded);
      queue.off("retried", onAdded);
      queue.off("cancelled", onCancelled);
      const busy = () => slots.filter((slot) => slot.job !== undefined || slot.reserved);
      const deadline = stopOptions.timeoutMs === undefined ? Infinity : Date.now() + stopOptions.timeoutMs;
      while (busy().length > 0 && Date.now() < deadline) {
        await new Promise((resolve) => setTimeout(resolve, 10));
      }
      for (const slot of busy()) slot.ending = "released";
      for (const slot of [...slots]) {
        slot.ending ??= "stopped";
        void slot.worker.terminate();
      }
      await Promise.all(slots.map((slot) => slot.exited));
      while (pending.size > 0) await Promise.allSettled([...pending]);
      started = false;
      checkDrained();
    },
  };
}
