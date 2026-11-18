import assert from "node:assert/strict";
import { mkdtemp, readdir, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { after, describe, test } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { fileSink } from "../service.ts";
import { event, events, post, postChunks, startService, until, writtenTypes } from "./ingest.ts";

const dirs: string[] = [];
after(async () => {
  for (const dir of dirs) await rm(dir, { recursive: true, force: true });
});

describe("POST /events", () => {
  test("202 with the counts; the sink gets each event as compact JSON", async () => {
    const { port, service, sink } = await startService();
    const reply = await post(port, `{ "type": "click", "time": "2026-03-01T12:00:00Z", "x": 1 }\n${event("view")}\n`);
    assert.equal(reply.status, 202, reply.text);
    assert.match(String(reply.headers["content-type"]), /^application\/json/);
    assert.deepEqual(reply.json, { accepted: 2, rejected: 0, errors: [] });
    await service.flush();
    assert.deepEqual(sink.batches, [['{"type":"click","time":"2026-03-01T12:00:00Z","x":1}', event("view")]]);
  });

  test("rejects invalid lines with their 1-based line numbers and keeps the valid ones", async () => {
    const { port, service, sink } = await startService();
    const body = [
      event("ok1"),
      "{not json",
      "",
      "[1, 2]",
      JSON.stringify({ time: "2026-03-01T12:00:00Z" }),
      JSON.stringify({ type: "", time: "2026-03-01T12:00:00Z" }),
      JSON.stringify({ type: "x".repeat(101), time: "2026-03-01T12:00:00Z" }),
      JSON.stringify({ type: "late", time: "yesterday-ish" }),
      JSON.stringify({ type: 7, time: "2026-03-01T12:00:00Z" }),
      "   ",
      event("ok2"),
    ].join("\n");
    const reply = await post(port, `${body}\n`);
    assert.equal(reply.status, 202);
    assert.equal(reply.json.accepted, 2);
    assert.equal(reply.json.rejected, 7);
    assert.deepEqual(reply.json.errors.map((error: { line: number }) => error.line), [2, 4, 5, 6, 7, 8, 9]);
    for (const error of reply.json.errors) assert.ok(typeof error.message === "string" && error.message !== "");
    await service.flush();
    assert.deepEqual(writtenTypes(sink), ["ok1", "ok2"]);
  });

  test("lists at most 10 errors but counts them all", async () => {
    const { port } = await startService();
    const reply = await post(port, "nope\n".repeat(15));
    assert.equal(reply.json.rejected, 15);
    assert.equal(reply.json.errors.length, 10);
  });

  test("lines split across chunks, a character split across chunks, CRLF and a last line without a newline", async () => {
    const { port, service, sink } = await startService();
    const cafe = Buffer.from(`${event("café")}\n`);
    const cut = cafe.indexOf(Buffer.from("é")) + 1; // in the middle of the two bytes of é
    const reply = await postChunks(
      port,
      ['{"type":"sp', 'lit","time":"2026-03-01T12:00:00Z"}\n', cafe.subarray(0, cut), cafe.subarray(cut), `${event("crlf")}\r\n`, event("last")],
      { gapMs: 20 },
    );
    assert.deepEqual(reply.json, { accepted: 4, rejected: 0, errors: [] });
    await service.flush();
    assert.deepEqual(writtenTypes(sink), ["split", "café", "crlf", "last"]);
  });

  test("a line longer than maxLineBytes is rejected and the lines after it still count", async () => {
    const { port, service, sink } = await startService({ maxLineBytes: 200 });
    const long = event("long", { pad: "x".repeat(1000) });
    const reply = await postChunks(port, [`${event("a")}\n`, long.slice(0, 500), long.slice(500), `\n${event("b")}\n`], { gapMs: 10 });
    assert.equal(reply.json.accepted, 2);
    assert.equal(reply.json.rejected, 1);
    assert.equal(reply.json.errors[0]?.line, 2);
    await service.flush();
    assert.deepEqual(writtenTypes(sink), ["a", "b"]);
  });

  test("415 for another content type, 405 with Allow for another method, 404 elsewhere", async () => {
    const { port, base } = await startService();
    assert.equal((await post(port, `${event("a")}\n`, { contentType: "application/json" })).status, 415);
    const get = await fetch(`${base}/events`);
    assert.equal(get.status, 405);
    assert.equal(get.headers.get("allow"), "POST");
    assert.equal((await fetch(`${base}/nope`)).status, 404);
  });
});

describe("batches", () => {
  test("a full batch goes to the sink at once, without waiting for flush; order is kept", async () => {
    const { port, sink } = await startService({ batchSize: 3, flushMs: 60_000 });
    await post(port, events(7));
    await until(() => sink.batches.length === 2, 2000, "two full batches");
    assert.deepEqual(sink.batches.map((batch) => batch.length), [3, 3]);
    assert.deepEqual(writtenTypes(sink), ["e0", "e1", "e2", "e3", "e4", "e5"]);
  });

  test("flush() writes the open batch and resolves once everything is written", async () => {
    const { port, service, sink } = await startService({ batchSize: 3, flushMs: 60_000 });
    await post(port, events(7));
    await service.flush();
    assert.deepEqual(sink.batches.map((batch) => batch.length), [3, 3, 1]);
    await service.flush();
    assert.equal(sink.batches.length, 3, "flush with nothing open writes no empty batch");
  });

  test("an open batch is written flushMs after its first event", async () => {
    const { port, sink } = await startService({ batchSize: 100, flushMs: 150 });
    await post(port, events(2));
    await sleep(50);
    assert.equal(sink.batches.length, 0, "too early");
    await until(() => sink.batches.length === 1, 1000, "the timed flush");
    assert.deepEqual(writtenTypes(sink), ["e0", "e1"]);
  });

  test("events from requests at the same time all arrive; the sink is called one batch at a time", async () => {
    const { port, service, sink } = await startService({ batchSize: 40 });
    const replies = await Promise.all([post(port, events(55, "a")), post(port, events(45, "b")), post(port, events(30, "c"))]);
    assert.deepEqual(replies.map((reply) => reply.json.accepted), [55, 45, 30]);
    await service.flush();
    const types = writtenTypes(sink);
    assert.equal(types.length, 130);
    for (const prefix of ["a", "b", "c"]) {
      const mine = types.filter((type) => type.startsWith(prefix));
      assert.deepEqual(mine, mine.slice().sort((x, y) => Number(x.slice(1)) - Number(y.slice(1))), `${prefix} events stay in order`);
    }
    assert.equal(sink.maxConcurrent, 1);
  });
});

describe("fileSink", () => {
  test("writes batch-000001.ndjson, batch-000002.ndjson... one event per line, and continues after a restart", async () => {
    const dir = path.join(await mkdtemp(path.join(tmpdir(), "ingest-")), "out");
    dirs.push(path.dirname(dir));
    const sink = fileSink(dir);
    await sink.write(['{"n":1}', '{"n":2}']);
    await sink.write(['{"n":3}']);
    assert.deepEqual((await readdir(dir)).sort(), ["batch-000001.ndjson", "batch-000002.ndjson"]);
    assert.equal(await readFile(path.join(dir, "batch-000001.ndjson"), "utf8"), '{"n":1}\n{"n":2}\n');
    await writeFile(path.join(dir, "notes.txt"), "not a batch");
    await fileSink(dir).write(['{"n":4}']);
    assert.deepEqual((await readdir(dir)).sort(), ["batch-000001.ndjson", "batch-000002.ndjson", "batch-000003.ndjson", "notes.txt"]);
  });

  test("works behind the service", async () => {
    const dir = await mkdtemp(path.join(tmpdir(), "ingest-"));
    dirs.push(dir);
    const { port, service } = await startService({ sink: fileSink(dir) as never, batchSize: 2 });
    await post(port, events(5));
    await service.flush();
    const files = (await readdir(dir)).sort();
    assert.deepEqual(files, ["batch-000001.ndjson", "batch-000002.ndjson", "batch-000003.ndjson"]);
    const lines = (await Promise.all(files.map((file) => readFile(path.join(dir, file), "utf8")))).join("").trim().split("\n");
    assert.deepEqual(lines.map((line) => JSON.parse(line).type), ["e0", "e1", "e2", "e3", "e4"]);
  });
});
