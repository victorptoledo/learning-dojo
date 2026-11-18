// The job handlers the contract tests run on your worker threads (the module's default export).
import { appendFileSync } from "node:fs";
import { setTimeout as sleep } from "node:timers/promises";
import { threadId } from "node:worker_threads";

type Context = { id: string; attempt: number };

/** Keeps the thread busy: CPU work, which only a worker thread can take off the event loop. */
function busy(ms: number): void {
  const end = Date.now() + ms;
  while (Date.now() < end) {
    // spin
  }
}

const handlers: Record<string, (payload: never, context: Context) => unknown> = {
  echo: (payload: unknown) => payload,
  square: (payload: { n: number }) => payload.n * payload.n,
  where: () => ({ threadId }),
  spin: (payload: { ms: number }) => {
    const start = Date.now();
    busy(payload.ms);
    return { start, end: Date.now(), threadId };
  },
  nap: async (payload: { ms: number }) => {
    await sleep(payload.ms);
    return "rested";
  },
  fail: (payload: { message: string }) => {
    throw new Error(payload.message);
  },
  reject: async (payload: { message: string }) => {
    await sleep(5);
    throw new Error(payload.message);
  },
  exit: (payload: { code: number }) => process.exit(payload.code),
  hang: () => {
    for (;;) {
      // never returns: only terminating the worker stops it
    }
  },
  /** Appends "<attempt> <time>" to payload.file, and fails while attempt < failUntil. */
  log: (payload: { file: string; failUntil: number }, context: Context) => {
    appendFileSync(payload.file, `${context.attempt} ${Date.now()}\n`);
    if (context.attempt < payload.failUntil) throw new Error(`attempt ${context.attempt} failed`);
    return context.attempt;
  },
};

export default handlers;
