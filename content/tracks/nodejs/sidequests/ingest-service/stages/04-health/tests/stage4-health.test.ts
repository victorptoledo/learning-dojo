import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { events, post, startService, until, writtenTypes } from "./ingest.ts";
import { scrape } from "./prometheus.ts";

async function health(base: string, which: "live" | "ready") {
  const response = await fetch(`${base}/health/${which}`);
  return { status: response.status, body: await response.json() };
}

describe("health checks", () => {
  test("live is always 200; ready is 200 while nothing is wrong", async () => {
    const { base } = await startService();
    assert.deepEqual(await health(base, "live"), { status: 200, body: { status: "ok" } });
    assert.deepEqual(await health(base, "ready"), { status: 200, body: { status: "ready" } });
  });

  test("ready is 503 with the reason while maxPending batches wait (live stays 200)", async () => {
    const { base, port, service, sink } = await startService({ batchSize: 5, maxPending: 2, flushMs: 60_000 });
    sink.close();
    const upload = post(port, events(12));
    await until(() => service.stats().pendingBatches === 2, 2000, "two pending batches");
    assert.deepEqual(await health(base, "ready"), { status: 503, body: { status: "not ready", reasons: ["backpressure"] } });
    assert.equal((await health(base, "live")).status, 200);
    sink.open();
    await upload;
    await service.flush();
    assert.equal((await health(base, "ready")).status, 200);
  });
});

describe("a failing sink", () => {
  test("a failed write is retried until it works: nothing lost, nothing written twice, order kept", async () => {
    const { port, service, sink } = await startService({ batchSize: 4, retryMs: 20, flushMs: 60_000 });
    sink.failNext(3);
    await post(port, events(10));
    await service.flush();
    assert.deepEqual(writtenTypes(sink), ["e0", "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9"]);
    assert.deepEqual(sink.batches.map((batch) => batch.length), [4, 4, 2]);
    assert.equal(sink.calls, 6, "3 failed calls and 3 that worked");
    assert.equal(sink.maxConcurrent, 1, "the next batch waits while one is being retried");
  });

  test("retries back off: retryMs, then twice that, then four times...", async () => {
    const { port, service, sink } = await startService({ batchSize: 100, retryMs: 100 });
    sink.failNext(3);
    await post(port, events(3));
    const started = Date.now();
    await service.flush();
    const took = Date.now() - started;
    assert.ok(took >= 600, `3 failures with retryMs 100 should wait about 100 + 200 + 400 ms, waited ${took}`);
    assert.ok(took < 3000, `waited ${took} ms`);
    assert.equal(sink.batches.length, 1);
  });

  test("ready says the sink is failing until a write works; failures are counted", async () => {
    const { base, port, service, sink } = await startService({ batchSize: 2, retryMs: 300, flushMs: 60_000 });
    sink.failNext(1);
    await post(port, events(2));
    await until(() => sink.calls === 1, 2000, "the first write");
    await new Promise((resolve) => setTimeout(resolve, 50));
    const failing = await health(base, "ready");
    assert.equal(failing.status, 503);
    assert.deepEqual(failing.body, { status: "not ready", reasons: ["sink failing"] });
    await service.flush();
    assert.deepEqual(await health(base, "ready"), { status: 200, body: { status: "ready" } });
    const metrics = await scrape(base);
    assert.equal(metrics.types.get("ingest_sink_failures_total"), "counter");
    assert.equal(metrics.value("ingest_sink_failures_total"), 1);
    assert.equal(metrics.value("ingest_batches_written_total"), 1);
  });
});
