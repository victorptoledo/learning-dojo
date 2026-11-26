// Runs on a worker thread: the chained SHA-256 of one file, posted back as hex.
import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import { parentPort, workerData } from "node:worker_threads";

/** sha256(content), then sha256 of the previous 32 bytes, `rounds` times in all. */
export function chainedSha256(content: Uint8Array, rounds: number): string {
  let digest = createHash("sha256").update(content).digest();
  for (let round = 1; round < rounds; round += 1) {
    digest = createHash("sha256").update(digest).digest();
  }
  return digest.toString("hex");
}

if (parentPort !== null) {
  const { file, rounds } = workerData as { file: string; rounds: number };
  // A rejection here becomes the worker's "error" event in the main thread.
  parentPort.postMessage(chainedSha256(await readFile(file), rounds));
}
