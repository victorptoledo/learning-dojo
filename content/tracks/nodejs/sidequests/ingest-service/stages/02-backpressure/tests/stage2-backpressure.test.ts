import assert from "node:assert/strict";
import { once } from "node:events";
import { request } from "node:http";
import { describe, test } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { events, post, startService, until, writtenTypes, type Reply } from "./ingest.ts";

/** Opens POST /events and returns the request and its reply. */
function openPost(port: number) {
  const req = request({
    host: "127.0.0.1",
    port,
    path: "/events",
    method: "POST",
    agent: false,
    headers: { "content-type": "application/x-ndjson" },
  });
  const reply = new Promise<Reply>((resolve, reject) => {
    req.on("response", (res) => {
      let text = "";
      res.setEncoding("utf8");
      res.on("data", (chunk: string) => (text += chunk));
      res.on("end", () => resolve({ status: res.statusCode ?? 0, headers: res.headers, text, json: JSON.parse(text || "null") }));
    });
    req.on("error", reject);
  });
  return { req, reply };
}

/**
 * Writes event lines as fast as the server takes them, up to `limit` bytes. Returns how much
 * was written and whether the writes stalled: write() said "wait" and no "drain" came for
 * 400 ms, because the server stopped reading.
 */
async function pushUntilStalled(req: ReturnType<typeof request>, limit: number) {
  const chunk = Buffer.from(events(500, "x"));
  let bytes = 0;
  let lines = 0;
  while (bytes < limit) {
    const ok = req.write(chunk);
    bytes += chunk.length;
    lines += 500;
    if (!ok) {
      const drained = await Promise.race([once(req, "drain").then(() => true), sleep(400).then(() => false)]);
      if (!drained) return { bytes, lines, stalled: true };
    }
  }
  return { bytes, lines, stalled: false };
}

describe("stats", () => {
  test("counts the open batch, the pending batches and the batches written", async () => {
    const { port, service, sink } = await startService({ batchSize: 10, flushMs: 60_000 });
    assert.deepEqual(service.stats(), { bufferedEvents: 0, pendingBatches: 0, batchesWritten: 0 });
    sink.close();
    await post(port, events(25));
    await until(() => service.stats().pendingBatches === 2, 2000, "two pending batches");
    assert.deepEqual(service.stats(), { bufferedEvents: 25, pendingBatches: 2, batchesWritten: 0 });
    sink.open();
    await service.flush();
    assert.deepEqual(service.stats(), { bufferedEvents: 0, pendingBatches: 0, batchesWritten: 3 });
  });
});

describe("backpressure", () => {
  test("while the sink is stuck, the service stops reading the body, so the client is slowed down", async () => {
    const { port, service, sink } = await startService({ batchSize: 100, maxPending: 2, flushMs: 60_000 });
    sink.close();
    const { req, reply } = openPost(port);
    const pushed = await pushUntilStalled(req, 40 * 1024 * 1024);
    assert.ok(pushed.stalled, `the server read all ${pushed.bytes} bytes while its sink was stuck: no backpressure`);
    assert.ok(pushed.bytes < 25 * 1024 * 1024, `${pushed.bytes} bytes went in before the writes stalled`);
    const stats = service.stats();
    assert.equal(stats.pendingBatches, 2, "maxPending batches wait for the sink, no more");
    assert.ok(stats.bufferedEvents <= 300, `${stats.bufferedEvents} events wait in memory`);
    sink.open();
    req.end();
    const answer = await reply;
    assert.equal(answer.status, 202);
    assert.equal(answer.json.accepted, pushed.lines, "nothing was lost");
    await service.flush();
    const types = writtenTypes(sink);
    assert.equal(types.length, pushed.lines);
    assert.equal(sink.maxConcurrent, 1);
  });

  test("a new request while maxPending batches wait gets 503 with Retry-After, and works again later", async () => {
    const { port, service, sink } = await startService({ batchSize: 5, maxPending: 1, flushMs: 60_000 });
    sink.close();
    const first = post(port, events(12, "a"));
    await until(() => service.stats().pendingBatches === 1, 2000, "a pending batch");
    const shed = await post(port, events(3, "b"));
    assert.equal(shed.status, 503);
    assert.equal(shed.headers["retry-after"], "1");
    sink.open();
    assert.equal((await first).json.accepted, 12);
    await service.flush();
    const later = await post(port, events(3, "c"));
    assert.equal(later.status, 202);
    await service.flush();
    assert.deepEqual(writtenTypes(sink).filter((type) => type.startsWith("b")), [], "a shed request stores nothing");
    assert.equal(writtenTypes(sink).length, 15);
  });

  test("maxPending is 4 by default", async () => {
    const { port, service, sink } = await startService({ batchSize: 2, flushMs: 60_000 });
    sink.close();
    const first = post(port, events(20));
    await until(() => service.stats().pendingBatches === 4, 2000, "four pending batches");
    await sleep(100);
    assert.equal(service.stats().pendingBatches, 4);
    assert.equal((await post(port, events(1))).status, 503);
    sink.open();
    assert.equal((await first).json.accepted, 20);
  });
});
