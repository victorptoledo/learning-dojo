// The worker thread: answers one Request at a time with a Reply (see protocol.ts).
// Listen with parentPort (from "node:worker_threads") and answer with parentPort.postMessage().
// Keep this file free of top-level code other than that listener: the tests import countPrimes.

/** The number of primes smaller than `below`. */
export function countPrimes(below: number): number {
  throw new Error("todo: write countPrimes()");
}
