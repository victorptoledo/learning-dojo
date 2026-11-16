// The event loop demo for predict.ts. Read it, but do not change it.
import { readFile } from "node:fs";

/** Runs the callbacks below from inside an I/O callback and resolves with the order they ran in. */
export function runDemo(): Promise<string[]> {
  return new Promise((resolve) => {
    const log: string[] = [];
    readFile(import.meta.filename, () => {
      log.push("A");                                   // the I/O callback itself
      setTimeout(() => log.push("B"), 0);
      setImmediate(() => log.push("C"));
      process.nextTick(() => log.push("D"));
      Promise.resolve().then(() => log.push("E"));
      queueMicrotask(() => log.push("F"));
      log.push("G");
      setTimeout(() => resolve(log), 30);
    });
  });
}
