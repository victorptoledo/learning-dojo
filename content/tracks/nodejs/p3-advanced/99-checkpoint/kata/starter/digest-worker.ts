// Runs on a worker thread (see digestInWorker in runner.ts): reads `workerData.file`, computes
// its chained SHA-256 with `workerData.rounds` rounds and posts the hex string to the main
// thread. Useful imports: createHash from "node:crypto", readFile from "node:fs/promises",
// parentPort and workerData from "node:worker_threads".

/** sha256(content), then sha256 of the previous 32 bytes, `rounds` times in all, as hex. */
export function chainedSha256(content: Uint8Array, rounds: number): string {
  throw new Error("todo: write chainedSha256()");
}

// todo: when parentPort is not null, read the file and post chainedSha256(...) back.
