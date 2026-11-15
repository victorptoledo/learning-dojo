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
  if (value instanceof Error) {
    return value;
  }
  if (typeof value === "string") {
    return new Error(value);
  }
  if (typeof value === "object" && value !== null) {
    try {
      return new Error(JSON.stringify(value));
    } catch {
      return new Error(String(value));
    }
  }
  return new Error(String(value));
}

/** Resolves after ms; rejects with signal.reason when the signal is (or becomes) aborted. */
export function sleep(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(signal.reason);
      return;
    }
    const onAbort = () => {
      clearTimeout(timer);
      reject(signal?.reason);
    };
    const timer = setTimeout(() => {
      signal?.removeEventListener("abort", onAbort);
      resolve();
    }, ms);
    signal?.addEventListener("abort", onAbort, { once: true });
  });
}

/**
 * Runs task with a signal. After ms, aborts the signal and rejects with a TimeoutError
 * ("timed out after <ms> ms"). Otherwise settles like the task, and leaves no timer behind.
 */
export function withTimeout<T>(task: (signal: AbortSignal) => Promise<T>, ms: number): Promise<T> {
  const controller = new AbortController();
  return new Promise<T>((resolve, reject) => {
    const timer = setTimeout(() => {
      const error = new TimeoutError(`timed out after ${ms} ms`);
      controller.abort(error);
      reject(error);
    }, ms);
    task(controller.signal)
      .then(resolve, reject)
      .finally(() => clearTimeout(timer));
  });
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
  const { attempts, delayMs, factor = 2, shouldRetry = () => true, wait = sleep } = options;
  const errors: unknown[] = [];
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      return await task(attempt);
    } catch (error) {
      if (!shouldRetry(error)) {
        throw error;
      }
      errors.push(error);
      if (attempt < attempts) {
        await wait(delayMs * factor ** (attempt - 1));
      }
    }
  }
  throw new RetryError(errors);
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
  const results = new Array<R>(items.length);
  let next = 0;
  let failed = false;
  const worker = async () => {
    while (!failed && next < items.length) {
      const index = next;
      next += 1;
      try {
        results[index] = await fn(items[index] as T, index);
      } catch (error) {
        failed = true;
        throw error;
      }
    }
  };
  const workers = Array.from({ length: Math.min(limit, items.length) }, worker);
  await Promise.all(workers);
  return results;
}
