import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { createRunner, type Job } from "../queue.ts";
import { queueIn, reopen } from "./helpers.ts";
import { HANDLERS, withRunner, within } from "./runners.ts";

describe("cancel", () => {
  test("a queued job is cancelled and never runs; finished jobs cannot be cancelled", async () => {
    const { dir, queue } = await queueIn();
    const doomed = await queue.add("square", { n: 2 });
    const kept = await queue.add("square", { n: 3 });
    const seen: Job[] = [];
    queue.on("cancelled", (job: Job) => seen.push(job));
    assert.equal(await queue.cancel(doomed.id), true);
    assert.deepEqual(seen.map((job) => job.id), [doomed.id]);
    assert.equal(queue.get(doomed.id)?.state, "cancelled");
    assert.equal((await queue.take())?.id, kept.id);
    await queue.complete(kept.id, 9);
    assert.equal(await queue.cancel(kept.id), false);
    assert.equal(await queue.cancel(doomed.id), false);
    await assert.rejects(queue.cancel("404"));
    const again = await reopen(dir, queue);
    assert.equal(again.get(doomed.id)?.state, "cancelled");
  });

  test("a running job is stopped at once: its worker is terminated and the next jobs still run", async () => {
    const { queue } = await queueIn();
    const stuck = await queue.add("hang", null, { maxAttempts: 3 });
    await queue.add("square", { n: 6 });
    await withRunner(queue, { concurrency: 1, timeoutMs: 30_000 }, async (runner) => {
      runner.start();
      await sleep(100);
      assert.equal(queue.get(stuck.id)?.state, "running");
      const started = Date.now();
      assert.equal(await queue.cancel(stuck.id), true);
      await within(runner.drain(), 3000, "drain after cancelling a job that never ends");
      assert.ok(Date.now() - started < 3000);
    });
    const [cancelled, next] = queue.list();
    assert.equal(cancelled?.state, "cancelled", "a cancelled job is not failed or retried afterwards");
    assert.equal(cancelled?.attempts, 1);
    assert.equal(next?.result, 36);
  });

  test("a cancelled job's late answer is ignored", async () => {
    const { queue } = await queueIn();
    const job = await queue.add("nap", { ms: 150 });
    await withRunner(queue, { concurrency: 1 }, async (runner) => {
      runner.start();
      await sleep(50);
      await queue.cancel(job.id);
      await sleep(250);
      await within(runner.drain(), 3000, "drain");
    });
    assert.equal(queue.get(job.id)?.state, "cancelled");
    assert.equal(queue.get(job.id)?.result, undefined);
  });
});

describe("release", () => {
  test("puts a running job back without counting the attempt; other states are refused", async () => {
    const { queue } = await queueIn();
    const job = await queue.add("a", null);
    await assert.rejects(queue.release(job.id));
    await queue.take();
    const released = await queue.release(job.id);
    assert.equal(released.state, "queued");
    assert.equal(released.attempts, 0);
    assert.equal((await queue.take())?.attempts, 1);
  });
});

describe("stop with a deadline", () => {
  test("waits up to timeoutMs, then puts the jobs still running back in the queue", async () => {
    const { dir, queue } = await queueIn();
    await queue.add("nap", { ms: 5000 });
    await queue.add("hang", null);
    await queue.add("square", { n: 2 });
    const runner = createRunner(queue, { handlers: HANDLERS, concurrency: 2 });
    runner.start();
    await sleep(150);
    const started = Date.now();
    await within(runner.stop({ timeoutMs: 200 }), 3000, "stop({ timeoutMs: 200 })");
    const took = Date.now() - started;
    assert.ok(took >= 180, `stop returned after ${took} ms, before the deadline`);
    const states = queue.list().map((job) => [job.state, job.attempts]);
    assert.deepEqual(states, [["queued", 0], ["queued", 0], ["queued", 0]]);
    const again = await reopen(dir, queue);
    assert.deepEqual(again.list().map((job) => [job.state, job.attempts]), states);
  });

  test("jobs that finish before the deadline are done, and stop returns as soon as they are", async () => {
    const { queue } = await queueIn();
    await queue.add("nap", { ms: 150 });
    const runner = createRunner(queue, { handlers: HANDLERS, concurrency: 1 });
    runner.start();
    await sleep(50);
    const started = Date.now();
    await within(runner.stop({ timeoutMs: 5000 }), 3000, "stop");
    assert.ok(Date.now() - started < 1500, "stop waited for the deadline although nothing was running");
    assert.equal(queue.list()[0]?.state, "done");
  });

  test("a stopped runner can be followed by a new one that finishes the work", async () => {
    const { queue } = await queueIn();
    for (let i = 1; i <= 4; i++) await queue.add("square", { n: i });
    await queue.add("nap", { ms: 3000 });
    const first = createRunner(queue, { handlers: HANDLERS, concurrency: 1 });
    first.start();
    await sleep(30);
    await first.stop({ timeoutMs: 50 });
    await withRunner(queue, { concurrency: 2 }, async (runner) => {
      runner.start();
      assert.equal(await queue.cancel("5"), true, "the long nap was put back, so it can still be cancelled");
      await within(runner.drain(), 5000, "drain");
    });
    assert.deepEqual(queue.list().slice(0, 4).map((job) => job.result), [1, 4, 9, 16]);
  });
});
