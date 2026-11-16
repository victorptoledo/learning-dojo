// The worker thread: answers one Request at a time with a Reply (see protocol.ts).
import { parentPort } from "node:worker_threads";
import type { Reply, Request, Task } from "./protocol.ts";

/** The number of primes smaller than `below` (a sieve of Eratosthenes). */
export function countPrimes(below: number): number {
  if (below < 3) {
    return 0;
  }
  const composite = new Uint8Array(below);
  let count = 0;
  for (let n = 2; n < below; n += 1) {
    if (composite[n] === 0) {
      count += 1;
      for (let multiple = n * n; multiple < below; multiple += n) {
        composite[multiple] = 1;
      }
    }
  }
  return count;
}

function spin(ms: number): number {
  const end = performance.now() + ms;
  while (performance.now() < end) {
    // busy on purpose: this is the CPU-bound work that would block the main thread
  }
  return ms;
}

function perform(task: Task): number {
  switch (task.kind) {
    case "countPrimes":
      return countPrimes(task.below);
    case "spin":
      return spin(task.ms);
    case "fail":
      throw new Error(task.message);
    case "crash":
      process.exit(task.code); // in a worker, this ends only the worker thread
  }
}

parentPort?.on("message", ({ id, task }: Request) => {
  let reply: Reply;
  try {
    reply = { id, ok: true, value: perform(task) };
  } catch (error) {
    reply = { id, ok: false, error: error instanceof Error ? error.message : String(error) };
  }
  parentPort?.postMessage(reply);
});
