import assert from "node:assert/strict";
import { EventEmitter } from "node:events";
import { describe, test } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { runDemo } from "../demo.ts";
import { predictedOrder } from "../predict.ts";
import { JobQueue, processInChunks, waitFor } from "../queue.ts";

describe("predict", () => {
  test("the predicted order matches the event loop", async () => {
    assert.deepEqual([...predictedOrder], await runDemo());
  });
});

describe("JobQueue", () => {
  test("rejects a concurrency below 1", () => {
    assert.throws(() => new JobQueue(async () => 1, 0), RangeError);
    assert.throws(() => new JobQueue(async () => 1, 1.5), RangeError);
  });

  test("gives ids 1, 2, 3 and starts nothing inside push()", async () => {
    const queue = new JobQueue(async (n: number) => n);
    const started: number[] = [];
    const ids = [queue.push(10), queue.push(20), queue.push(30)];
    assert.deepEqual(ids, [1, 2, 3]);
    assert.equal(queue.running, 0, "push() must not start the job synchronously");
    assert.equal(queue.waiting, 3);
    queue.on("start", (id) => started.push(id)); // added after push(): still sees every start
    await queue.idle();
    assert.deepEqual(started, [1, 2, 3]);
    assert.equal(queue.waiting, 0);
  });

  test("never runs more than concurrency jobs and keeps the order", async () => {
    let active = 0;
    let peak = 0;
    const order: number[] = [];
    const queue = new JobQueue(async (n: number) => {
      active += 1;
      peak = Math.max(peak, active);
      order.push(n);
      await sleep(5);
      active -= 1;
      return n;
    }, 2);
    for (let n = 1; n <= 7; n += 1) {
      queue.push(n);
    }
    await queue.idle();
    assert.equal(peak, 2);
    assert.deepEqual(order, [1, 2, 3, 4, 5, 6, 7]);
  });

  test("reports failures with 'failed', keeps going and never emits 'error'", async () => {
    const queue = new JobQueue(async (n: number) => {
      if (n === 2) {
        throw new Error("two is bad");
      }
      if (n === 3) {
        throw "a string"; // not an Error: the queue must wrap it
      }
      return n;
    });
    const failed: [number, string, boolean][] = [];
    const done: number[] = [];
    queue.on("failed", (id, error) => failed.push([id, error.message, error instanceof Error]));
    queue.on("done", (id) => done.push(id));
    [1, 2, 3, 4].forEach((n) => queue.push(n));
    await queue.idle();
    assert.deepEqual(done, [1, 4]);
    assert.deepEqual(failed, [
      [2, "two is bad", true],
      [3, "a string", true],
    ]);
  });

  test("emits 'drain' once when everything is finished", async () => {
    const queue = new JobQueue(async (n: number) => {
      await sleep(n);
      return n;
    }, 3);
    let drains = 0;
    queue.on("drain", () => {
      drains += 1;
    });
    [5, 1, 3, 2].forEach((n) => queue.push(n));
    await queue.idle();
    await sleep(10);
    assert.equal(drains, 1);
    assert.equal(queue.running, 0);
  });

  test("idle() resolves at once when there is nothing to do", async () => {
    const queue = new JobQueue(async () => 1);
    const result = await Promise.race([queue.idle().then(() => "idle"), sleep(50, "stuck")]);
    assert.equal(result, "idle");
  });
});

describe("waitFor", () => {
  test("resolves with the event's arguments and removes its listener", async () => {
    const emitter = new EventEmitter();
    setTimeout(() => emitter.emit("ready", 1, "two"), 5);
    assert.deepEqual(await waitFor(emitter, "ready", 1000), [1, "two"]);
    assert.equal(emitter.listenerCount("ready"), 0);
    assert.equal(emitter.listenerCount("error"), 0);
  });

  test("rejects with the error when 'error' comes first", async () => {
    const emitter = new EventEmitter();
    setTimeout(() => emitter.emit("error", new Error("broken")), 5);
    await assert.rejects(waitFor(emitter, "ready", 1000), { message: "broken" });
  });

  test("rejects after the timeout and leaves no listener", async () => {
    const emitter = new EventEmitter();
    const started = performance.now();
    await assert.rejects(waitFor(emitter, "ready", 30), {
      message: 'timed out waiting for "ready" after 30 ms',
    });
    assert.ok(performance.now() - started >= 25);
    assert.equal(emitter.listenerCount("ready"), 0);
  });
});

describe("processInChunks", () => {
  test("visits every item in order and counts the chunks", async () => {
    const seen: [string, number][] = [];
    const chunks = await processInChunks(["a", "b", "c", "d", "e"], (item, index) => seen.push([item, index]), 2);
    assert.equal(chunks, 3);
    assert.deepEqual(seen, [
      ["a", 0],
      ["b", 1],
      ["c", 2],
      ["d", 3],
      ["e", 4],
    ]);
    assert.equal(await processInChunks([], () => {}, 4), 0);
  });

  test("lets setImmediate callbacks run between chunks", async () => {
    let ticks = 0;
    const tick = () => {
      ticks += 1;
      if (ticks < 100) {
        setImmediate(tick);
      }
    };
    setImmediate(tick);
    const ticksAt: number[] = [];
    await processInChunks([...Array(6).keys()], () => ticksAt.push(ticks), 2);
    assert.equal(ticksAt[0], ticksAt[1], "one chunk runs without interruption");
    assert.ok((ticksAt[2] ?? 0) > (ticksAt[1] ?? 0), "the loop must turn between chunks");
    assert.ok((ticksAt[4] ?? 0) > (ticksAt[3] ?? 0), "the loop must turn between chunks");
  });

  test("rejects a chunk size below 1", async () => {
    await assert.rejects(processInChunks([1], () => {}, 0), RangeError);
  });
});
