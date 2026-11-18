// Starts a runner for one test and always stops it, so no worker thread outlives the test.
import { createRunner, type Queue, type Runner, type RunnerOptions } from "../queue.ts";

export const HANDLERS = new URL("./handlers.ts", import.meta.url);

export async function withRunner(
  queue: Queue,
  options: Partial<RunnerOptions>,
  run: (runner: Runner) => Promise<void>,
): Promise<void> {
  const runner = createRunner(queue, { handlers: HANDLERS, ...options });
  try {
    await run(runner);
  } finally {
    await runner.stop();
  }
}

/** Rejects when the promise takes longer than ms. */
export function within<T>(promise: Promise<T>, ms: number, what: string): Promise<T> {
  let timer: NodeJS.Timeout | undefined;
  const timeout = new Promise<never>((_, reject) => {
    timer = setTimeout(() => reject(new Error(`${what} took more than ${ms} ms`)), ms);
  });
  return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
}
