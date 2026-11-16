// A job queue built on EventEmitter, plus two helpers that work with the event loop.
// Useful imports: `once` from "node:events", `setImmediate` from "node:timers/promises".
import { EventEmitter } from "node:events";

/** The events of a JobQueue and the arguments of each one. */
export interface QueueEvents<R> {
  start: [id: number];
  done: [id: number, result: R];
  failed: [id: number, error: Error];
  drain: [];
}

/**
 * Runs jobs with an async worker, at most `concurrency` at a time, in the order they were
 * pushed. Reports progress with events; a failing job never emits "error".
 */
export class JobQueue<T, R> extends EventEmitter<QueueEvents<R>> {
  constructor(worker: (job: T) => Promise<R>, concurrency = 1) {
    super();
    throw new Error("todo: write the JobQueue constructor (and add private fields)");
  }

  /** Jobs running now. */
  get running(): number {
    throw new Error("todo: write running");
  }

  /** Jobs pushed but not started yet. */
  get waiting(): number {
    throw new Error("todo: write waiting");
  }

  /** Adds a job and returns its id (1, 2, 3 ...). It starts later, never inside push(). */
  push(job: T): number {
    throw new Error("todo: write push()");
  }

  /** Resolves when no job is running or waiting. */
  async idle(): Promise<void> {
    throw new Error("todo: write idle()");
  }
}

/**
 * Resolves with the arguments of the next `event` of the emitter. Rejects with the error when
 * the emitter emits "error" first, and with `timed out waiting for "<event>" after <ms> ms`
 * when nothing happens in time. Leaves no listener behind.
 */
export async function waitFor(emitter: EventEmitter, event: string, ms: number): Promise<unknown[]> {
  throw new Error("todo: write waitFor()");
}

/**
 * Calls fn(item, index) for every item, `chunkSize` items at a time, and lets the event loop
 * run other callbacks (timers, I/O, setImmediate) between chunks. Resolves with the number of
 * chunks.
 */
export async function processInChunks<T>(
  items: readonly T[],
  fn: (item: T, index: number) => void,
  chunkSize: number,
): Promise<number> {
  throw new Error("todo: write processInChunks()");
}
