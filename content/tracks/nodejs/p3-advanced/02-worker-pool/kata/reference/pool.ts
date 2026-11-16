// A fixed-size pool of worker threads for CPU-bound tasks.
import { availableParallelism } from "node:os";
import { Worker } from "node:worker_threads";
import type { Reply, Request, Task } from "./protocol.ts";

interface Job {
  id: number;
  task: Task;
  resolve: (value: number) => void;
  reject: (error: Error) => void;
}

interface Slot {
  worker: Worker;
  job: Job | undefined;
}

export const WORKER_FILE = new URL("./worker.ts", import.meta.url);

/**
 * Runs tasks on `size` worker threads, one task per worker at a time, the others queued in
 * order. A worker that exits while running a task fails that task and is replaced.
 */
export class WorkerPool {
  readonly #file: URL;
  readonly #slots: Slot[] = [];
  readonly #queue: Job[] = [];
  #nextId = 1;
  #closed = false;

  constructor(size = availableParallelism(), file: URL = WORKER_FILE) {
    if (!Number.isInteger(size) || size < 1) {
      throw new RangeError(`size must be a whole number of at least 1, got ${size}`);
    }
    this.#file = file;
    for (let index = 0; index < size; index += 1) {
      this.#slots.push(this.#spawn());
    }
  }

  /** Runs the task on the next free worker and resolves with its value. */
  run(task: Task): Promise<number> {
    if (this.#closed) {
      return Promise.reject(new Error("pool closed"));
    }
    return new Promise((resolve, reject) => {
      this.#queue.push({ id: this.#nextId, task, resolve, reject });
      this.#nextId += 1;
      this.#dispatch();
    });
  }

  /** Workers, workers running a task, and tasks waiting for a worker. */
  stats(): { size: number; busy: number; queued: number } {
    return {
      size: this.#slots.length,
      busy: this.#slots.filter((slot) => slot.job !== undefined).length,
      queued: this.#queue.length,
    };
  }

  /** Rejects the queued and running tasks with "pool closed" and stops every worker. */
  async close(): Promise<void> {
    if (this.#closed) {
      return;
    }
    this.#closed = true;
    const closed = new Error("pool closed");
    for (const job of this.#queue.splice(0)) {
      job.reject(closed);
    }
    await Promise.all(
      this.#slots.map(async (slot) => {
        slot.job?.reject(closed);
        slot.job = undefined;
        await slot.worker.terminate();
      }),
    );
  }

  #spawn(): Slot {
    const slot: Slot = { worker: new Worker(this.#file), job: undefined };
    slot.worker.on("message", (reply: Reply) => {
      const job = slot.job;
      if (job === undefined || job.id !== reply.id) {
        return;
      }
      slot.job = undefined;
      if (reply.ok) {
        job.resolve(reply.value);
      } else {
        job.reject(new Error(reply.error));
      }
      this.#dispatch();
    });
    slot.worker.on("error", (error: unknown) => this.#replace(slot, error instanceof Error ? error : new Error(String(error))));
    slot.worker.on("exit", (code) => this.#replace(slot, new Error(`worker exited with code ${code}`)));
    return slot;
  }

  /** The worker of this slot is gone: fail its task and start a new worker in its place. */
  #replace(slot: Slot, error: Error): void {
    if (this.#closed) {
      return;
    }
    const index = this.#slots.indexOf(slot);
    if (index === -1) {
      return; // already replaced (an "error" is followed by an "exit")
    }
    slot.job?.reject(error);
    slot.worker.removeAllListeners();
    slot.worker.on("error", () => {}); // the old worker is going away; ignore its last words
    void slot.worker.terminate();
    this.#slots[index] = this.#spawn();
    this.#dispatch();
  }

  #dispatch(): void {
    for (const slot of this.#slots) {
      if (slot.job !== undefined) {
        continue;
      }
      const job = this.#queue.shift();
      if (job === undefined) {
        return;
      }
      slot.job = job;
      slot.worker.postMessage({ id: job.id, task: job.task } satisfies Request);
    }
  }
}
