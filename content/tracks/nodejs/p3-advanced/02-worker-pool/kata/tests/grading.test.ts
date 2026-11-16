import assert from "node:assert/strict";
import { after, before, describe, test } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { WorkerPool } from "../pool.ts";
import { countPrimes } from "../worker.ts";

describe("countPrimes", () => {
  test("counts the primes below n", () => {
    assert.deepEqual(
      [0, 2, 3, 10, 100, 1000, 100_000].map(countPrimes),
      [0, 0, 1, 4, 25, 168, 9592],
    );
  });
});

describe("WorkerPool", () => {
  let pool: WorkerPool;
  before(() => {
    pool = new WorkerPool(2);
  });
  after(() => pool.close());

  test("rejects a size below 1", () => {
    assert.throws(() => new WorkerPool(0), RangeError);
  });

  test("runs tasks on worker threads and resolves with their values", async () => {
    const values = await Promise.all([100, 1000, 10_000, 100_000].map((below) => pool.run({ kind: "countPrimes", below })));
    assert.deepEqual(values, [25, 168, 1229, 9592]);
  });

  test("runs at most size tasks at a time and queues the rest", async () => {
    const running = [1, 2, 3, 4, 5].map(() => pool.run({ kind: "spin", ms: 60 }));
    assert.deepEqual(pool.stats(), { size: 2, busy: 2, queued: 3 });
    assert.deepEqual(await Promise.all(running), [60, 60, 60, 60, 60]);
    assert.deepEqual(pool.stats(), { size: 2, busy: 0, queued: 0 });
  });

  test("keeps the main thread free while the workers are busy", async () => {
    let ticks = 0;
    const timer = setInterval(() => {
      ticks += 1;
    }, 10);
    await Promise.all([pool.run({ kind: "spin", ms: 300 }), pool.run({ kind: "spin", ms: 300 })]);
    clearInterval(timer);
    assert.ok(ticks >= 10, `the event loop only turned ${ticks} times in 300 ms`);
  });

  test("a task that fails rejects with its message and the worker keeps working", async () => {
    await assert.rejects(pool.run({ kind: "fail", message: "bad input" }), { message: "bad input" });
    assert.equal(await pool.run({ kind: "countPrimes", below: 10 }), 4);
  });

  test("a crashed worker fails its task, is replaced, and the queue goes on", async () => {
    const crash = pool.run({ kind: "crash", code: 3 });
    const next = [pool.run({ kind: "countPrimes", below: 100 }), pool.run({ kind: "countPrimes", below: 1000 }), pool.run({ kind: "countPrimes", below: 10 })];
    await assert.rejects(crash, { message: "worker exited with code 3" });
    assert.deepEqual(await Promise.all(next), [25, 168, 4]);
    assert.equal(pool.stats().size, 2);
    const again = await Promise.all([1, 2, 3, 4].map(() => pool.run({ kind: "countPrimes", below: 30 })));
    assert.deepEqual(again, [10, 10, 10, 10]);
  });
});

describe("close", () => {
  test("rejects queued tasks and later runs with 'pool closed'", async () => {
    const pool = new WorkerPool(1);
    const running = pool.run({ kind: "spin", ms: 200 });
    const queued = assert.rejects(pool.run({ kind: "countPrimes", below: 100 }), { message: "pool closed" });
    running.catch(() => {});
    await sleep(20);
    await pool.close();
    await queued;
    await assert.rejects(pool.run({ kind: "countPrimes", below: 100 }), { message: "pool closed" });
    assert.equal(pool.stats().queued, 0);
  });

  test("lets the process exit: no worker is left running", async () => {
    const pool = new WorkerPool(2);
    assert.equal(await pool.run({ kind: "countPrimes", below: 100 }), 25);
    await pool.close();
    assert.equal(pool.stats().busy, 0);
  });
});
