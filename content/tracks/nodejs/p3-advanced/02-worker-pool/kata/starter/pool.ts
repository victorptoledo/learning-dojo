// A fixed-size pool of worker threads for CPU-bound tasks.
// Useful imports: Worker from "node:worker_threads", availableParallelism from "node:os".
import type { Task } from "./protocol.ts";

export const WORKER_FILE = new URL("./worker.ts", import.meta.url);

/**
 * Runs tasks on `size` worker threads, one task per worker at a time, the others queued in
 * order. A worker that exits while running a task fails that task and is replaced.
 */
export class WorkerPool {
  constructor(size?: number, file: URL = WORKER_FILE) {
    throw new Error("todo: write the WorkerPool constructor (and add private fields)");
  }

  /** Runs the task on the next free worker and resolves with its value. */
  run(task: Task): Promise<number> {
    throw new Error("todo: write run()");
  }

  /** Workers, workers running a task, and tasks waiting for a worker. */
  stats(): { size: number; busy: number; queued: number } {
    throw new Error("todo: write stats()");
  }

  /** Rejects the queued and running tasks with "pool closed" and stops every worker. */
  async close(): Promise<void> {
    throw new Error("todo: write close()");
  }
}
