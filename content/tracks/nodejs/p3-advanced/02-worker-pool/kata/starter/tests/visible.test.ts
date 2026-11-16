import assert from "node:assert/strict";
import { test } from "node:test";
import { WorkerPool } from "../pool.ts";

test("counts primes on a worker thread", async () => {
  const pool = new WorkerPool(1);
  try {
    assert.equal(await pool.run({ kind: "countPrimes", below: 100 }), 25);
  } finally {
    await pool.close();
  }
});
