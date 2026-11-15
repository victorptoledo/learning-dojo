// Async helpers with typed errors: sleeping, timeouts, retries and limited concurrency.

/** Thrown by withTimeout when the task takes too long. */
export class TimeoutError extends Error {
  override name = "TimeoutError";
}

/** Thrown by retry when every attempt failed: `cause` is the last error. */
export class RetryError extends Error {
  override name = "RetryError";
  readonly attempts: number;
  readonly errors: readonly unknown[];

  constructor(errors: readonly unknown[]) {
    super(`failed after ${errors.length} attempts`, { cause: errors.at(-1) });
    this.attempts = errors.length;
    this.errors = errors;
  }
}

/** A thrown value as an Error: an Error stays as it is, anything else becomes the message. */
export function toError(value: unknown): Error {
  throw new Error("todo: write toError()");
}

/** Resolves after ms; rejects with signal.reason when the signal is (or becomes) aborted. */
export function sleep(ms: number, signal?: AbortSignal): Promise<void> {
  throw new Error("todo: write sleep()");
}

/**
 * Runs task with a signal. After ms, aborts the signal and rejects with a TimeoutError
 * ("timed out after <ms> ms"). Otherwise settles like the task, and leaves no timer behind.
 */
export function withTimeout<T>(task: (signal: AbortSignal) => Promise<T>, ms: number): Promise<T> {
  throw new Error("todo: write withTimeout()");
}

export interface RetryOptions {
  /** The total number of attempts, at least 1. */
  attempts: number;
  /** The wait before the second attempt. */
  delayMs: number;
  /** Each later wait is multiplied by this (default 2). */
  factor?: number;
  /** Return false to stop retrying and rethrow that error as it is (default: always retry). */
  shouldRetry?: (error: unknown) => boolean;
  /** How to wait (default: sleep). Tests replace it to record the delays. */
  wait?: (ms: number) => Promise<void>;
}

/** Calls task(1), task(2), ... until one resolves; a RetryError when all attempts fail. */
export async function retry<T>(task: (attempt: number) => Promise<T>, options: RetryOptions): Promise<T> {
  throw new Error("todo: write retry()");
}

/**
 * Maps every item with fn, with at most `limit` calls running at a time. Resolves with the
 * results in the order of the items; rejects with the first error and starts no new calls.
 */
export async function mapLimit<T, R>(
  items: readonly T[],
  limit: number,
  fn: (item: T, index: number) => Promise<R>,
): Promise<R[]> {
  throw new Error("todo: write mapLimit()");
}
