import assert from "node:assert/strict";
import type { AddressInfo } from "node:net";
import { PassThrough } from "node:stream";
import type { IncomingMessage, Server } from "node:http";
import { after, before, describe, test } from "node:test";
import { createApp, HttpError, readJson, validateNote } from "../app.ts";

let server: Server;
let base = "";

async function start(): Promise<void> {
  server = createApp();
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  base = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
}

async function stop(): Promise<void> {
  server.closeAllConnections();
  await new Promise<void>((resolve) => server.close(() => resolve()));
}

/** The parsed JSON body; `any` keeps the assertions short (fetch types it as unknown). */
const json = async (response: Response): Promise<any> => response.json();

const post = (path: string, body: string, type = "application/json") =>
  fetch(`${base}${path}`, { method: "POST", headers: { "content-type": type }, body });

/** A fake request: a stream with headers, enough for readJson. */
function fakeRequest(body: string, type = "application/json"): IncomingMessage {
  const stream = new PassThrough();
  stream.end(body);
  return Object.assign(stream, { headers: { "content-type": type } }) as unknown as IncomingMessage;
}

describe("readJson", () => {
  test("parses a JSON body", async () => {
    assert.deepEqual(await readJson(fakeRequest('{"a":1}')), { a: 1 });
  });

  test("415, 413 and 400 as HttpError", async () => {
    const status = async (request: IncomingMessage, limit?: number) => {
      try {
        await readJson(request, limit);
        return 0;
      } catch (error) {
        assert.ok(error instanceof HttpError, `expected an HttpError, got ${String(error)}`);
        return error.status;
      }
    };
    assert.equal(await status(fakeRequest('{"a":1}', "text/plain")), 415);
    assert.equal(await status(fakeRequest('{"text":"long enough"}'), 10), 413);
    assert.equal(await status(fakeRequest("{oops")), 400);
    assert.equal(await status(fakeRequest("   ")), 400);
    assert.equal(await status(fakeRequest('{"a":1}', "application/json; charset=utf-8")), 0);
  });
});

describe("validateNote", () => {
  test("trims the text and rejects bad bodies with 422", () => {
    assert.equal(validateNote({ text: "  buy milk " }), "buy milk");
    for (const body of [{}, { text: 1 }, { text: "   " }, { text: "x".repeat(281) }, null, "text", []]) {
      assert.throws(() => validateNote(body), (error: unknown) => error instanceof HttpError && error.status === 422);
    }
    assert.equal(validateNote({ text: "x".repeat(280) }).length, 280);
  });
});

describe("the API", () => {
  before(start);
  after(stop);

  test("GET /health", async () => {
    const response = await fetch(`${base}/health`);
    assert.equal(response.status, 200);
    assert.equal(response.headers.get("content-type"), "application/json; charset=utf-8");
    assert.deepEqual(await json(response), { status: "ok" });
  });

  test("POST /notes creates notes with ids, Location and 201", async () => {
    const first = await post("/notes", JSON.stringify({ text: " Learn streams " }));
    assert.equal(first.status, 201);
    assert.equal(first.headers.get("location"), "/notes/1");
    assert.deepEqual(await json(first), { id: 1, text: "Learn streams" });
    const second = await post("/notes", JSON.stringify({ text: "Learn HTTP" }));
    assert.deepEqual(await json(second), { id: 2, text: "Learn HTTP" });
  });

  test("GET /notes lists in id order and filters with ?q=", async () => {
    await post("/notes", JSON.stringify({ text: "Write tests" }));
    const all = await json(await fetch(`${base}/notes`));
    assert.deepEqual(
      all.notes.map((note: { id: number }) => note.id),
      [1, 2, 3],
    );
    const learn = await json(await fetch(`${base}/notes?q=LEARN`));
    assert.deepEqual(learn, {
      notes: [
        { id: 1, text: "Learn streams" },
        { id: 2, text: "Learn HTTP" },
      ],
    });
  });

  test("GET and DELETE /notes/:id", async () => {
    assert.deepEqual(await json(await fetch(`${base}/notes/2`)), { id: 2, text: "Learn HTTP" });
    const deleted = await fetch(`${base}/notes/2`, { method: "DELETE" });
    assert.equal(deleted.status, 204);
    assert.equal(await deleted.text(), "");
    const gone = await fetch(`${base}/notes/2`);
    assert.equal(gone.status, 404);
    assert.equal(typeof (await json(gone)).error, "string");
    assert.equal((await fetch(`${base}/notes/2`, { method: "DELETE" })).status, 404);
  });

  test("the errors of a bad POST", async () => {
    const statuses = [
      (await post("/notes", "{not json")).status,
      (await post("/notes", JSON.stringify({ text: "" }))).status,
      (await post("/notes", "text=hi", "application/x-www-form-urlencoded")).status,
      (await post("/notes", JSON.stringify({ text: "x".repeat(20_000) }))).status,
    ];
    assert.deepEqual(statuses, [400, 422, 415, 413]);
    const body = await json(await post("/notes", "{not json"));
    assert.equal(typeof body.error, "string");
  });

  test("405 with Allow for a wrong method", async () => {
    const onList = await fetch(`${base}/notes`, { method: "PUT" });
    assert.equal(onList.status, 405);
    assert.equal(onList.headers.get("allow"), "GET, POST");
    const onNote = await fetch(`${base}/notes/1`, { method: "PATCH" });
    assert.equal(onNote.status, 405);
    assert.equal(onNote.headers.get("allow"), "GET, DELETE");
    assert.equal((await fetch(`${base}/health`, { method: "POST" })).headers.get("allow"), "GET");
  });

  test("404 as JSON for unknown paths and ids", async () => {
    for (const path of ["/", "/nope", "/notes/abc", "/notes/1/extra", "/notes/999"]) {
      const response = await fetch(`${base}${path}`);
      assert.equal(response.status, 404, path);
      assert.equal(response.headers.get("content-type"), "application/json; charset=utf-8");
      assert.equal(typeof (await json(response)).error, "string");
    }
  });
});

describe("a fresh app", () => {
  before(start);
  after(stop);

  test("starts empty with ids from 1: no state shared between apps", async () => {
    assert.deepEqual(await json(await fetch(`${base}/notes`)), { notes: [] });
    assert.equal((await json(await post("/notes", JSON.stringify({ text: "again" })))).id, 1);
  });
});
