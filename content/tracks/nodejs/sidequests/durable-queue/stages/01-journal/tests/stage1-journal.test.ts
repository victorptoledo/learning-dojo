import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { appendFile, readdir, readFile, stat } from "node:fs/promises";
import path from "node:path";
import { describe, test } from "node:test";
import { JOURNAL, type Job } from "../queue.ts";
import { queueIn, reopen } from "./helpers.ts";

const summary = (jobs: Job[]) => jobs.map((job) => `${job.id}:${job.type}:${job.state}:${job.attempts}`);

describe("add, take, complete and fail", () => {
  test("add returns the queued job with an id, and get and list return copies of it", async () => {
    const { queue } = await queueIn();
    const before = Date.now();
    const job = await queue.add("email", { to: "ana@example.com" });
    assert.equal(job.id, "1");
    assert.equal(job.type, "email");
    assert.deepEqual(job.payload, { to: "ana@example.com" });
    assert.equal(job.priority, 0);
    assert.equal(job.state, "queued");
    assert.equal(job.attempts, 0);
    assert.ok(job.createdAt >= before && job.createdAt <= Date.now());
    assert.equal((await queue.add("report", null, { priority: 2 })).id, "2");
    assert.deepEqual(queue.get("1"), job);
    assert.equal(queue.get("99"), undefined);
    const listed = queue.list();
    assert.deepEqual(summary(listed), ["1:email:queued:0", "2:report:queued:0"]);
    (listed[0]?.payload as { to: string }).to = "changed outside";
    assert.deepEqual(queue.get("1")?.payload, { to: "ana@example.com" }, "list() must not hand out the queue's own objects");
  });

  test("take returns the highest priority first, then the oldest, and marks it running", async () => {
    const { queue } = await queueIn();
    await queue.add("a", 1);
    await queue.add("b", 2, { priority: 10 });
    await queue.add("c", 3);
    await queue.add("d", 4, { priority: 10 });
    const order: string[] = [];
    for (let job = await queue.take(); job !== undefined; job = await queue.take()) {
      assert.equal(job.state, "running");
      assert.equal(job.attempts, 1);
      order.push(job.type);
    }
    assert.deepEqual(order, ["b", "d", "a", "c"]);
    assert.deepEqual(summary(queue.list("running")).length, 4);
    assert.equal(await queue.take(), undefined);
  });

  test("complete stores the result, fail stores the error; only a running job can finish", async () => {
    const { queue } = await queueIn();
    await queue.add("ok", null);
    await queue.add("bad", null);
    const ok = await queue.take();
    const bad = await queue.take();
    const done = await queue.complete(ok?.id ?? "", { sent: 3 });
    assert.equal(done.state, "done");
    assert.deepEqual(done.result, { sent: 3 });
    const failed = await queue.fail(bad?.id ?? "", "smtp is down");
    assert.equal(failed.state, "failed");
    assert.equal(failed.error, "smtp is down");
    await assert.rejects(queue.complete(ok?.id ?? "", "again"));
    await assert.rejects(queue.fail("99", "no such job"));
    await queue.add("waiting", null);
    await assert.rejects(queue.complete("3", "never taken"));
    assert.deepEqual(summary(queue.list("done")), ["1:ok:done:1"]);
    assert.deepEqual(summary(queue.list("failed")), ["2:bad:failed:1"]);
  });

  test("emits added with the new job", async () => {
    const { queue } = await queueIn();
    const seen: Job[] = [];
    queue.on("added", (job: Job) => seen.push(job));
    await queue.add("ping", { n: 1 });
    assert.deepEqual(seen.map((job) => [job.id, job.type]), [["1", "ping"]]);
  });

  test("twenty takes at once never hand out the same job twice", async () => {
    const { queue } = await queueIn();
    for (let i = 0; i < 5; i++) await queue.add("job", i);
    const taken = (await Promise.all(Array.from({ length: 20 }, () => queue.take()))).filter((job) => job !== undefined);
    assert.equal(taken.length, 5);
    assert.equal(new Set(taken.map((job) => job.id)).size, 5);
  });
});

describe("the journal", () => {
  test("a reopened queue has the same jobs, and new ids continue after the old ones", async () => {
    const { dir, queue } = await queueIn();
    await queue.add("a", { n: 1 });
    await queue.add("b", { n: 2 }, { priority: 3 });
    await queue.add("c", [1, "two"]);
    const b = await queue.take();
    await queue.complete(b?.id ?? "", "b done");
    const a = await queue.take();
    await queue.fail(a?.id ?? "", "a broke");
    const before = queue.list();
    const again = await reopen(dir, queue);
    assert.deepEqual(again.list(), before);
    assert.equal((await again.add("d", null)).id, "4");
    const third = await reopen(dir, again);
    assert.deepEqual(summary(third.list()), ["1:a:failed:1", "2:b:done:1", "3:c:queued:0", "4:d:queued:0"]);
  });

  test("lives in journal.ndjson, one JSON object per line", async () => {
    const { dir, queue } = await queueIn();
    await queue.add("a", null);
    const job = await queue.take();
    await queue.complete(job?.id ?? "", 42);
    const text = await readFile(path.join(dir, JOURNAL), "utf8");
    assert.ok(text.endsWith("\n"));
    const lines = text.trimEnd().split("\n");
    assert.ok(lines.length >= 1);
    for (const line of lines) assert.equal(typeof JSON.parse(line), "object");
  });

  test("fifty adds at once are all there after a reopen", async () => {
    const { dir, queue } = await queueIn();
    const jobs = await Promise.all(Array.from({ length: 50 }, (_, i) => queue.add("bulk", { i })));
    assert.equal(new Set(jobs.map((job) => job.id)).size, 50);
    const before = queue.list();
    const again = await reopen(dir, queue);
    assert.deepEqual(again.list(), before);
    assert.deepEqual(
      again.list().map((job) => (job.payload as { i: number }).i).sort((x, y) => x - y),
      Array.from({ length: 50 }, (_, i) => i),
    );
  });

  test("a process killed with SIGKILL loses nothing, and its running job is queued again", async () => {
    const { dir, queue: unused } = await queueIn();
    await unused.close();
    const child = spawn(process.execPath, [path.join(import.meta.dirname, "crash-child.ts"), dir], { stdio: ["ignore", "pipe", "pipe"] });
    let out = "";
    let err = "";
    child.stdout.on("data", (chunk: Buffer) => (out += chunk));
    child.stderr.on("data", (chunk: Buffer) => (err += chunk));
    const signal = await new Promise<NodeJS.Signals | null>((resolve) => child.on("close", (_code, sig) => resolve(sig)));
    assert.equal(signal, "SIGKILL", `the child did not get to its SIGKILL: ${err}`);
    assert.equal(out, "took 2\n");
    const { queue } = await queueIn(dir);
    assert.deepEqual(summary(queue.list()), ["1:email:queued:0", "2:email:queued:1", "3:report:queued:0"]);
    assert.equal((await queue.take())?.id, "2", "the job that was running keeps its priority");
  });

  test("a torn last line (a write cut short) is ignored, and later writes still work", async () => {
    const { dir, queue } = await queueIn();
    await queue.add("a", 1);
    await queue.add("b", 2);
    await queue.close();
    await appendFile(path.join(dir, JOURNAL), '{"id":"3","type":"c","pay');
    const { queue: again } = await queueIn(dir);
    assert.deepEqual(summary(again.list()), ["1:a:queued:0", "2:b:queued:0"]);
    await again.add("c", 3);
    const third = await reopen(dir, again);
    assert.deepEqual(summary(third.list()), ["1:a:queued:0", "2:b:queued:0", "3:c:queued:0"]);
  });

  test("compact rewrites the journal smaller, keeps every job, and leaves no temporary file", async () => {
    const { dir, queue } = await queueIn();
    for (let i = 0; i < 40; i++) await queue.add("work", { i, text: "x".repeat(50) });
    for (let job = await queue.take(); job !== undefined; job = await queue.take()) {
      await queue.complete(job.id, { ok: job.id });
    }
    const file = path.join(dir, JOURNAL);
    const big = (await stat(file)).size;
    const before = queue.list();
    await queue.compact();
    assert.ok((await stat(file)).size < big / 2, "after compact the journal should be about one line per job");
    assert.deepEqual(await readdir(dir), [JOURNAL]);
    await queue.add("after", null);
    const again = await reopen(dir, queue);
    assert.deepEqual(again.list().slice(0, 40), before);
    assert.equal(again.list()[40]?.id, "41");
  });

  test("after close, changes are refused and nothing is lost", async () => {
    const { dir, queue } = await queueIn();
    const writes = [queue.add("a", 1), queue.add("b", 2), queue.add("c", 3)];
    await queue.close();
    await Promise.all(writes);
    await assert.rejects(queue.add("late", null));
    await assert.rejects(queue.take());
    const { queue: again } = await queueIn(dir);
    assert.deepEqual(summary(again.list()), ["1:a:queued:0", "2:b:queued:0", "3:c:queued:0"]);
  });
});
