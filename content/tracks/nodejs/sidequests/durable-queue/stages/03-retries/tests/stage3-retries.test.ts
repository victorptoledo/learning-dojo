import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { describe, test } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { queueIn, reopen, tempDir } from "./helpers.ts";
import { withRunner, within } from "./runners.ts";

/** The "<attempt> <time>" lines the log handler wrote. */
async function attemptsIn(file: string): Promise<{ attempt: number; at: number }[]> {
  const text = await readFile(file, "utf8").catch(() => "");
  return text
    .trim()
    .split("\n")
    .filter((line) => line !== "")
    .map((line) => {
      const [attempt, at] = line.split(" ").map(Number);
      return { attempt: attempt ?? 0, at: at ?? 0 };
    });
}

describe("fail with attempts left", () => {
  test("new jobs try once by default, with a backoff of 100 ms", async () => {
    const { queue } = await queueIn();
    const job = await queue.add("a", null);
    assert.equal(job.maxAttempts, 1);
    assert.equal(job.backoffMs, 100);
    assert.ok(Math.abs(job.runAt - job.createdAt) < 5, "a new job may run at once");
    await queue.take();
    assert.equal((await queue.fail(job.id, "once is enough")).state, "failed");
  });

  test("queues the job again with runAt = now + backoffMs * 2^(attempts - 1), and take waits for it", async () => {
    const { queue } = await queueIn();
    const job = await queue.add("flaky", null, { maxAttempts: 3, backoffMs: 200 });
    await queue.take();
    const before = Date.now();
    const first = await queue.fail(job.id, "first try");
    assert.equal(first.state, "queued");
    assert.equal(first.error, "first try");
    assert.ok(first.runAt >= before + 200 && first.runAt <= Date.now() + 200, `runAt ${first.runAt - before} ms ahead`);
    assert.equal(await queue.take(), undefined, "too early: the backoff has not passed");
    await sleep(first.runAt - Date.now() + 20);
    const second = await queue.take();
    assert.equal(second?.attempts, 2);
    const failedAgain = await queue.fail(job.id, "second try");
    assert.ok(failedAgain.runAt - Date.now() > 300, "the second backoff doubles to 400 ms");
    await sleep(failedAgain.runAt - Date.now() + 20);
    await queue.take();
    const last = await queue.fail(job.id, "third try");
    assert.equal(last.state, "failed");
    assert.equal(last.error, "third try");
    assert.equal(last.attempts, 3);
  });

  test("a waiting retry keeps its runAt across a restart", async () => {
    const { dir, queue } = await queueIn();
    const job = await queue.add("flaky", null, { maxAttempts: 2, backoffMs: 400 });
    await queue.take();
    const failed = await queue.fail(job.id, "try later");
    const again = await reopen(dir, queue);
    assert.equal(again.get(job.id)?.runAt, failed.runAt);
    assert.equal(await again.take(), undefined);
    await sleep(failed.runAt - Date.now() + 20);
    assert.equal((await again.take())?.id, job.id);
  });

  test("a job that is not ready does not block a ready one behind it", async () => {
    const { queue } = await queueIn();
    const slow = await queue.add("slow", null, { priority: 9, maxAttempts: 2, backoffMs: 5000 });
    await queue.take();
    await queue.fail(slow.id, "later");
    await queue.add("ready", null);
    assert.equal((await queue.take())?.type, "ready");
  });
});

describe("retry", () => {
  test("puts a failed job back from scratch; other states are refused", async () => {
    const { queue } = await queueIn();
    const job = await queue.add("a", null, { maxAttempts: 2 });
    await assert.rejects(queue.retry(job.id), "a queued job cannot be retried");
    await queue.take();
    await queue.fail(job.id, "one");
    await sleep(120);
    await queue.take();
    await queue.fail(job.id, "two");
    assert.equal(queue.get(job.id)?.state, "failed");
    const retried = await queue.retry(job.id);
    assert.equal(retried.state, "queued");
    assert.equal(retried.attempts, 0);
    assert.equal((await queue.take())?.id, job.id);
    await assert.rejects(queue.retry("404"));
  });
});

describe("the runner and retries", () => {
  test("waits the backoff between attempts, and drain waits for the retries", async () => {
    const { queue } = await queueIn();
    const file = path.join(await tempDir(), "attempts.log");
    await queue.add("log", { file, failUntil: 3 }, { maxAttempts: 4, backoffMs: 100 });
    await withRunner(queue, { concurrency: 1 }, async (runner) => {
      runner.start();
      await within(runner.drain(), 5000, "drain");
    });
    const [job] = queue.list();
    assert.equal(job?.state, "done");
    assert.equal(job?.result, 3);
    assert.equal(job?.attempts, 3);
    const attempts = await attemptsIn(file);
    assert.deepEqual(attempts.map((entry) => entry.attempt), [1, 2, 3]);
    const gaps = attempts.slice(1).map((entry, i) => entry.at - (attempts[i]?.at ?? 0));
    assert.ok((gaps[0] ?? 0) >= 95, `first gap ${gaps[0]} ms, expected at least 100`);
    assert.ok((gaps[1] ?? 0) >= 190, `second gap ${gaps[1]} ms, expected at least 200`);
  });

  test("ends in failed with the last error once the attempts run out", async () => {
    const { queue } = await queueIn();
    const file = path.join(await tempDir(), "attempts.log");
    await queue.add("log", { file, failUntil: 99 }, { maxAttempts: 3, backoffMs: 20 });
    await withRunner(queue, { concurrency: 1 }, async (runner) => {
      runner.start();
      await within(runner.drain(), 5000, "drain");
    });
    const [job] = queue.list();
    assert.equal(job?.state, "failed");
    assert.equal(job?.attempts, 3);
    assert.equal(job?.error, "attempt 3 failed");
    assert.equal((await attemptsIn(file)).length, 3);
  });

  test("a crashed worker and a timeout count as attempts too", async () => {
    const { queue } = await queueIn();
    await queue.add("exit", { code: 4 }, { maxAttempts: 2, backoffMs: 10 });
    await queue.add("hang", null, { maxAttempts: 2, backoffMs: 10 });
    await withRunner(queue, { concurrency: 2, timeoutMs: 200 }, async (runner) => {
      runner.start();
      await within(runner.drain(), 6000, "drain");
    });
    const [crashed, hung] = queue.list();
    assert.deepEqual([crashed?.state, crashed?.attempts], ["failed", 2]);
    assert.deepEqual([hung?.state, hung?.attempts], ["failed", 2]);
    assert.match(hung?.error ?? "", /timed out/);
  });

  test("a job retried by hand runs again", async () => {
    const { queue } = await queueIn();
    const file = path.join(await tempDir(), "attempts.log");
    const job = await queue.add("log", { file, failUntil: 2 });
    await withRunner(queue, { concurrency: 1 }, async (runner) => {
      runner.start();
      await within(runner.drain(), 5000, "drain");
      assert.equal(queue.get(job.id)?.state, "failed");
      await queue.retry(job.id);
      await within(runner.drain(), 5000, "drain after retry");
    });
    assert.equal(queue.get(job.id)?.state, "failed", "attempts start again at 1 after retry, so it fails again");
    assert.deepEqual((await attemptsIn(file)).map((entry) => entry.attempt), [1, 1]);
  });
});
