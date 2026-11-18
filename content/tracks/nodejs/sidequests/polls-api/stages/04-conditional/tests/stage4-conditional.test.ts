import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { assertProblem, errorPaths, pollBody, startApp, type Api } from "./api.ts";
import { race } from "./race.ts";

async function newPoll(api: Api, overrides: Record<string, unknown> = {}): Promise<string> {
  const reply = await api.post("/polls", pollBody(overrides));
  assert.equal(reply.status, 201, reply.text);
  return reply.body.id;
}

const patch = (api: Api, id: string, json: unknown, headers: Record<string, string>) =>
  api.call("PATCH", `/polls/${id}`, {
    body: JSON.stringify(json),
    headers: { "content-type": "application/merge-patch+json", ...headers },
  });

async function etagOf(api: Api, path: string): Promise<string> {
  const reply = await api.get(path);
  const etag = reply.headers.get("etag");
  assert.ok(etag !== null, `${path} has no ETag`);
  return etag;
}

describe("ETag and If-None-Match", () => {
  test("GET /polls/:id has a strong ETag that stays the same while the poll does", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    const etag = await etagOf(api, `/polls/${id}`);
    assert.match(etag, /^"[^"]+"$/, "a strong ETag is a quoted string without W/");
    assert.equal(await etagOf(api, `/polls/${id}`), etag);
    const other = await newPoll(api, { question: "Another one?" });
    assert.notEqual(await etagOf(api, `/polls/${other}`), etag);
  });

  test("304 without a body when If-None-Match has the current ETag, also in a list or as W/", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    const etag = await etagOf(api, `/polls/${id}`);
    for (const header of [etag, `"nope", ${etag}`, `W/${etag}`, "*"]) {
      const reply = await api.get(`/polls/${id}`, { "if-none-match": header });
      assert.equal(reply.status, 304, `If-None-Match: ${header}`);
      assert.equal(reply.text, "");
      assert.equal(reply.headers.get("etag"), etag);
    }
    const changed = await api.get(`/polls/${id}`, { "if-none-match": '"something-else"' });
    assert.equal(changed.status, 200);
    assert.equal(changed.body.id, id);
  });

  test("the ETag changes when the status changes with the clock", async () => {
    const api = await startApp();
    const id = await newPoll(api, { closesAt: "2026-03-01T12:00:10Z" });
    const open = await etagOf(api, `/polls/${id}`);
    api.advance(10_000);
    const reply = await api.get(`/polls/${id}`, { "if-none-match": open });
    assert.equal(reply.status, 200);
    assert.equal(reply.body.status, "closed");
  });

  test("results have an ETag too, and a new vote changes it", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    const before = await etagOf(api, `/polls/${id}/results`);
    assert.equal((await api.get(`/polls/${id}/results`, { "if-none-match": before })).status, 304);
    await api.post(`/polls/${id}/votes`, { optionId: 1 }, { "voter-id": "ana" });
    const after = await api.get(`/polls/${id}/results`, { "if-none-match": before });
    assert.equal(after.status, 200);
    assert.equal(after.body.total, 1);
    assert.notEqual(after.headers.get("etag"), before);
  });
});

describe("PATCH /polls/:id", () => {
  test("with the current ETag in If-Match: 200, the new poll and a new ETag", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    const etag = await etagOf(api, `/polls/${id}`);
    const reply = await patch(api, id, { question: "  Spaces or tabs? ", closesAt: "2026-03-05T00:00:00Z" }, { "if-match": etag });
    assert.equal(reply.status, 200, reply.text);
    assert.equal(reply.body.question, "Spaces or tabs?");
    assert.equal(reply.body.closesAt, "2026-03-05T00:00:00.000Z");
    const newEtag = reply.headers.get("etag");
    assert.ok(newEtag !== null && newEtag !== etag);
    assert.equal(await etagOf(api, `/polls/${id}`), newEtag);
    const removed = await patch(api, id, { closesAt: null }, { "if-match": newEtag });
    assert.equal(removed.status, 200, removed.text);
    assert.equal(removed.body.closesAt, null);
    assert.equal(removed.body.question, "Spaces or tabs?", "fields that are not sent stay as they were");
  });

  test("428 without If-Match, 412 with an old ETag (and the current one in the answer)", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    const etag = await etagOf(api, `/polls/${id}`);
    assertProblem(await patch(api, id, { question: "No precondition?" }, {}), 428);
    assert.equal((await patch(api, id, { question: "First writer" }, { "if-match": etag })).status, 200);
    const late = await patch(api, id, { question: "Second writer" }, { "if-match": etag });
    assertProblem(late, 412);
    assert.equal(late.headers.get("etag"), await etagOf(api, `/polls/${id}`));
    assert.equal((await api.get(`/polls/${id}`)).body.question, "First writer", "the lost update did not happen");
    assert.equal((await patch(api, id, { question: "Anyone" }, { "if-match": "*" })).status, 200);
  });

  test("two editors racing with the same ETag: exactly one wins", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    const etag = await etagOf(api, `/polls/${id}`);
    const replies = await race(
      api.base,
      "PATCH",
      `/polls/${id}`,
      Array.from({ length: 10 }, (_, i) => ({
        headers: { "content-type": "application/merge-patch+json", "if-match": etag },
        body: JSON.stringify({ question: `Editor ${i}` }),
      })),
    );
    assert.deepEqual(replies.map((reply) => reply.status).sort(), [200, ...Array.from({ length: 9 }, () => 412)]);
  });

  test("415 unless the body is application/merge-patch+json; 422 for invalid fields", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    const etag = await etagOf(api, `/polls/${id}`);
    const asJson = await api.call("PATCH", `/polls/${id}`, {
      body: JSON.stringify({ question: "Plain JSON?" }),
      headers: { "content-type": "application/json", "if-match": etag },
    });
    assertProblem(asJson, 415);
    const invalid = await patch(api, id, { question: "", closesAt: "2020-01-01T00:00:00Z", options: ["a", "b"], color: 1 }, { "if-match": etag });
    assertProblem(invalid, 422);
    assert.deepEqual(errorPaths(invalid), ["closesAt", "color", "options", "question"]);
    assertProblem(await patch(api, id, [1, 2], { "if-match": etag }), 422);
    assert.equal(await etagOf(api, `/polls/${id}`), etag, "a rejected PATCH changes nothing");
  });

  test("404 for an unknown poll; 405 on /polls/:id allows GET and PATCH", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    assertProblem(await patch(api, "nope", { question: "?" }, { "if-match": "*" }), 404);
    const reply = await api.call("DELETE", `/polls/${id}`);
    assertProblem(reply, 405);
    assert.equal(reply.headers.get("allow"), "GET, PATCH");
  });
});
