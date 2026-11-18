// The worker thread's entry point (new Worker(new URL("./worker.ts", import.meta.url), ...)).
// It loads the handlers module named in workerData once, then runs one job per message from
// parentPort and posts the outcome back. See the README of this stage.
// Useful: parentPort and workerData from node:worker_threads, and a dynamic import().

export {};
