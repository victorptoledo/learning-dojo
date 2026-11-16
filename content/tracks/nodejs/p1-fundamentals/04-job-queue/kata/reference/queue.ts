// A job queue built on EventEmitter, plus two helpers that work with the event loop.
import { EventEmitter, once } from "node:events";
import { setImmediate as nextLoopTurn } from "node:timers/promises";

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
  readonly #worker: (job: T) => Promise<R>;
  readonly #concurrency: number;
  readonly #waiting: { id: number; job: T }[] = [];
  #running = 0;
  #nextId = 1;

  constructor(worker: (job: T) => Promise<R>, concurrency = 1) {
    super();
    if (!Number.isInteger(concurrency) || concurrency < 1) {
      throw new RangeError(`concurrency must be a whole number of at least 1, got ${concurrency}`);
    }
    this.#worker = worker;
    this.#concurrency = concurrency;
  }

  /** Jobs running now. */
  get running(): number {
    return this.#running;
  }

  /** Jobs pushed but not started yet. */
  get waiting(): number {
    return this.#waiting.length;
  }

  /** Adds a job and returns its id (1, 2, 3 ...). It starts later, never inside push(). */
  push(job: T): number {
    const id = this.#nextId;
    this.#nextId += 1;
    this.#waiting.push({ id, job });
    process.nextTick(() => this.#startJobs());
    return id;
  }

  /** Resolves when no job is running or waiting. */
  async idle(): Promise<void> {
    if (this.#running === 0 && this.#waiting.length === 0) {
      return;
    }
    await once(this, "drain");
  }

  #startJobs(): void {
    while (this.#running < this.#concurrency) {
      const next = this.#waiting.shift();
      if (next === undefined) {
        return;
      }
      this.#running += 1;
      this.emit("start", next.id);
      this.#worker(next.job).then(
        (result) => this.#finish(() => this.emit("done", next.id, result)),
        (error: unknown) =>
          this.#finish(() =>
            this.emit("failed", next.id, error instanceof Error ? error : new Error(String(error))),
          ),
      );
    }
  }

  #finish(report: () => void): void {
    this.#running -= 1;
    report();
    this.#startJobs();
    if (this.#running === 0 && this.#waiting.length === 0) {
      this.emit("drain");
    }
  }
}

/**
 * Resolves with the arguments of the next `event` of the emitter. Rejects with the error when
 * the emitter emits "error" first, and with `timed out waiting for "<event>" after <ms> ms`
 * when nothing happens in time. Leaves no listener behind.
 */
export async function waitFor(emitter: EventEmitter, event: string, ms: number): Promise<unknown[]> {
  const signal = AbortSignal.timeout(ms);
  try {
    return await once(emitter, event, { signal });
  } catch (error) {
    if (signal.aborted) {
      throw new Error(`timed out waiting for "${event}" after ${ms} ms`, { cause: error });
    }
    throw error;
  }
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
  if (!Number.isInteger(chunkSize) || chunkSize < 1) {
    throw new RangeError(`chunkSize must be a whole number of at least 1, got ${chunkSize}`);
  }
  let chunks = 0;
  for (let start = 0; start < items.length; start += chunkSize) {
    if (chunks > 0) {
      await nextLoopTurn();
    }
    const end = Math.min(start + chunkSize, items.length);
    for (let index = start; index < end; index += 1) {
      fn(items[index] as T, index);
    }
    chunks += 1;
  }
  return chunks;
}
