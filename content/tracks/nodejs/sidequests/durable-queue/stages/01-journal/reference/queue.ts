// A durable job queue: every change is a line in an append-only journal, replayed on open.
import { EventEmitter } from "node:events";
import { mkdir, open, readFile, rename, truncate, type FileHandle } from "node:fs/promises";
import path from "node:path";

export type JobState = "queued" | "running" | "done" | "failed";

export interface Job {
  /** "1", "2", ...: unique and never reused, also across restarts. */
  id: string;
  type: string;
  payload: unknown;
  priority: number;
  state: JobState;
  /** How many times the job was taken. */
  attempts: number;
  createdAt: number;
  result?: unknown;
  error?: string;
}

export interface AddOptions {
  priority?: number;
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
      createdAt: now,
    };
    this.#jobs.set(job.id, job);
    await this.#save(job);
    this.emit("added", structuredClone(job));
    return structuredClone(job);
  }

  /** The next queued job: highest priority, then oldest. It becomes running. */
  async take(): Promise<Job | undefined> {
    if (this.#closed) throw new Error("the queue is closed");
    let best: Job | undefined;
    for (const job of this.#jobs.values()) {
      if (job.state !== "queued") continue;
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

  fail(id: string, error: string): Promise<Job> {
    return this.#change(id, ["running"], (job) => {
      job.state = "failed";
      job.error = error;
    });
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
