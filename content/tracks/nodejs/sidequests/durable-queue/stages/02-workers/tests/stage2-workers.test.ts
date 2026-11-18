import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { threadId as mainThread } from "node:worker_threads";
import { queueIn } from "./helpers.ts";
import { withRunner, within } from "./runners.ts";

type Spin = { start: number; end: number; threadId: number };

/** The largest number of intervals running at the same moment. */
function maxOverlap(intervals: Spin[]): number {
  return Math.max(...intervals.map((a) => intervals.filter((b) => b.start <= a.start && a.start < b.end).length));
}

describe("the runner", () => {
  test("runs every job and stores its result; drain resolves when nothing is left", async () => {
    const { queue } = await queueIn();
    await queue.add("square", { n: 7 });
    await queue.add("echo", { hello: ["world"] });
    await withRunner(queue, {}, async (runner) => {
      runner.start();
      await within(runner.drain(), 5000, "drain");
    });
    assert.deepEqual(
      queue.list().map((job) => [job.state, job.result]),
      [["done", 49], ["done", { hello: ["world"] }]],
    );
  });

  test("runs at most `concurrency` jobs at once, and really in parallel", async () => {
    const { queue } = await queueIn();
    for (let i = 0; i < 6; i++) await queue.add("spin", { ms: 250 });
    const started = Date.now();
    await withRunner(queue, { concurrency: 2 }, async (runner) => {
      runner.start();
      await within(runner.drain(), 8000, "drain");
    });
    const elapsed = Date.now() - started;
    const spins = queue.list().map((job) => job.result as Spin);
    assert.equal(maxOverlap(spins), 2);
    assert.ok(elapsed < 6 * 250 - 200, `6 jobs of 250 ms on 2 workers took ${elapsed} ms: they did not run in parallel`);
  });

  test("CPU-bound jobs do not block the main thread's event loop", async () => {
    const { queue } = await queueIn();
    for (let i = 0; i < 4; i++) await queue.add("spin", { ms: 300 });
    let ticks = 0;
    const ticker = setInterval(() => ticks++, 10);
    try {
      await withRunner(queue, { concurrency: 2 }, async (runner) => {
        runner.start();
        await within(runner.drain(), 8000, "drain");
      });
    } finally {
      clearInterval(ticker);
    }
    assert.ok(ticks >= 25, `only ${ticks} timer ticks in about 600 ms: the jobs ran on the main thread`);
    for (const job of queue.list()) assert.notEqual((job.result as Spin).threadId, mainThread);
  });

  test("keeps `concurrency` workers and reuses them: one thread per job is too slow", async () => {
    const { queue } = await queueIn();
    for (let i = 0; i < 12; i++) await queue.add("where", null);
    await withRunner(queue, { concurrency: 3 }, async (runner) => {
      runner.start();
      await within(runner.drain(), 8000, "drain");
    });
    const threads = new Set(queue.list().map((job) => (job.result as { threadId: number }).threadId));
    assert.ok(threads.size <= 3, `12 jobs ran on ${threads.size} threads`);
    assert.ok(!threads.has(mainThread));
  });

  test("picks up jobs added after start, without polling", async () => {
    const { queue } = await queueIn();
    await withRunner(queue, { concurrency: 1 }, async (runner) => {
      runner.start();
      await sleep(50);
      await queue.add("square", { n: 3 });
      await within(runner.drain(), 3000, "drain");
      await sleep(30);
      await queue.add("square", { n: 4 });
      await within(runner.drain(), 3000, "drain");
    });
    assert.deepEqual(queue.list().map((job) => job.result), [9, 16]);
  });

  test("takes jobs in priority order", async () => {
    const { queue } = await queueIn();
    await queue.add("spin", { ms: 20 }, { priority: 0 });
    await queue.add("spin", { ms: 20 }, { priority: 5 });
    await queue.add("spin", { ms: 20 }, { priority: 1 });
    await withRunner(queue, { concurrency: 1 }, async (runner) => {
      runner.start();
      await within(runner.drain(), 5000, "drain");
    });
    const starts = queue.list().map((job) => (job.result as Spin).start);
    assert.ok((starts[1] ?? 0) <= (starts[2] ?? 0) && (starts[2] ?? 0) <= (starts[0] ?? 0), `start times ${starts}`);
  });
});

describe("failures", () => {
  test("a handler that throws or rejects fails the job with its message; an unknown type fails too", async () => {
    const { queue } = await queueIn();
    await queue.add("fail", { message: "boom" });
    await queue.add("reject", { message: "async boom" });
    await queue.add("nope", null);
    await queue.add("echo", "still fine");
    await withRunner(queue, {}, async (runner) => {
      runner.start();
      await within(runner.drain(), 5000, "drain");
    });
    const jobs = queue.list();
    assert.deepEqual(jobs.map((job) => job.state), ["failed", "failed", "failed", "done"]);
    assert.equal(jobs[0]?.error, "boom");
    assert.equal(jobs[1]?.error, "async boom");
    assert.match(jobs[2]?.error ?? "", /nope/);
  });

  test("a worker that exits fails its job and is replaced", async () => {
    const { queue } = await queueIn();
    await queue.add("exit", { code: 3 });
    for (let i = 0; i < 4; i++) await queue.add("where", null);
    await withRunner(queue, { concurrency: 1 }, async (runner) => {
      runner.start();
      await within(runner.drain(), 5000, "drain");
    });
    const [crashed, ...rest] = queue.list();
    assert.equal(crashed?.state, "failed");
    assert.match(crashed?.error ?? "", /exit/);
    assert.match(crashed?.error ?? "", /3/);
    assert.deepEqual(rest.map((job) => job.state), ["done", "done", "done", "done"]);
  });

  test("a job that runs past timeoutMs is stopped: its worker is terminated and replaced", async () => {
    const { queue } = await queueIn();
    await queue.add("hang", null);
    await queue.add("square", { n: 5 });
    const started = Date.now();
    await withRunner(queue, { concurrency: 1, timeoutMs: 300 }, async (runner) => {
      runner.start();
      await within(runner.drain(), 5000, "drain");
    });
    const [hung, next] = queue.list();
    assert.equal(hung?.state, "failed");
    assert.match(hung?.error ?? "", /timed out/);
    assert.equal(next?.result, 25);
    assert.ok(Date.now() - started >= 280);
  });
});

describe("stop", () => {
  test("lets the running job finish, takes nothing new, and ends the workers", async () => {
    const { queue } = await queueIn();
    for (let i = 0; i < 4; i++) await queue.add("nap", { ms: 200 });
    await withRunner(queue, { concurrency: 1 }, async (runner) => {
      runner.start();
      await sleep(60);
      await within(runner.stop(), 3000, "stop");
    });
    assert.deepEqual(queue.list().map((job) => job.state), ["done", "queued", "queued", "queued"]);
    await sleep(100);
    assert.deepEqual(queue.list().map((job) => job.state), ["done", "queued", "queued", "queued"], "nothing runs after stop");
  });
});
