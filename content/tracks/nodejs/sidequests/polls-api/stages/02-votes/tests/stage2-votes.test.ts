import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { assertProblem, errorPaths, pollBody, startApp, type Api } from "./api.ts";
import { race } from "./race.ts";

async function newPoll(api: Api, overrides: Record<string, unknown> = {}): Promise<string> {
  const reply = await api.post("/polls", pollBody({ options: ["Red", "Green", "Blue"], ...overrides }));
  assert.equal(reply.status, 201, reply.text);
  return reply.body.id;
}

const vote = (api: Api, id: string, voter: string, optionId: unknown, headers: Record<string, string> = {}) =>
  api.post(`/polls/${id}/votes`, { optionId }, { "voter-id": voter, ...headers });

describe("POST /polls/:id/votes", () => {
  test("201 with the vote, and the results count it", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    const reply = await vote(api, id, "ana", 2);
    assert.equal(reply.status, 201, reply.text);
    assert.deepEqual(reply.body, { pollId: id, optionId: 2 });
    await vote(api, id, "bia", 2);
    await vote(api, id, "caio", 3);
    const results = await api.get(`/polls/${id}/results`);
    assert.equal(results.status, 200);
    assert.deepEqual(results.body, {
      pollId: id,
      status: "open",
      total: 3,
      options: [
        { id: 1, text: "Red", votes: 0 },
        { id: 2, text: "Green", votes: 2 },
        { id: 3, text: "Blue", votes: 1 },
      ],
      winners: [2],
    });
  });

  test("results of a poll without votes have no winner, and a tie has several", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    assert.deepEqual((await api.get(`/polls/${id}/results`)).body.winners, []);
    await vote(api, id, "ana", 1);
    await vote(api, id, "bia", 3);
    assert.deepEqual((await api.get(`/polls/${id}/results`)).body.winners, [1, 3]);
  });

  test("409 when the voter already voted on this poll, even for another option", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    const other = await newPoll(api);
    assert.equal((await vote(api, id, "ana", 1)).status, 201);
    assertProblem(await vote(api, id, "ana", 2), 409);
    assert.equal((await vote(api, other, "ana", 2)).status, 201, "a voter can vote on another poll");
    assert.equal((await api.get(`/polls/${id}/results`)).body.total, 1);
  });

  test("twenty votes of the same voter at once count once", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    const replies = await race(
      api.base,
      "POST",
      `/polls/${id}/votes`,
      Array.from({ length: 20 }, (_, i) => ({
        headers: { "content-type": "application/json", "voter-id": "eager" },
        body: JSON.stringify({ optionId: (i % 3) + 1 }),
      })),
    );
    assert.deepEqual(replies.map((reply) => reply.status).sort(), [201, ...Array.from({ length: 19 }, () => 409)]);
    assert.equal((await api.get(`/polls/${id}/results`)).body.total, 1);
  });

  test("409 once the poll is closed", async () => {
    const api = await startApp();
    const id = await newPoll(api, { closesAt: "2026-03-01T12:30:00Z" });
    assert.equal((await vote(api, id, "early", 1)).status, 201);
    api.advance(30 * 60 * 1000);
    assertProblem(await vote(api, id, "late", 1), 409);
    const results = (await api.get(`/polls/${id}/results`)).body;
    assert.equal(results.status, "closed");
    assert.equal(results.total, 1);
  });

  test("422 for an option the poll does not have; 400 without a valid voter-id", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    for (const optionId of [0, 4, "1", null, 1.5]) {
      const reply = await vote(api, id, "ana", optionId);
      assertProblem(reply, 422);
      assert.deepEqual(errorPaths(reply), ["optionId"], `optionId ${JSON.stringify(optionId)}`);
    }
    assertProblem(await api.post(`/polls/${id}/votes`, { optionId: 1 }), 400);
    assertProblem(await vote(api, id, "has spaces", 1), 400);
    assertProblem(await vote(api, id, "x".repeat(65), 1), 400);
    assert.equal((await vote(api, id, "Ana_99-b", 1)).status, 201);
  });

  test("404 for an unknown poll, 405 with Allow on the vote and results routes", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    assertProblem(await vote(api, "nope", "ana", 1), 404);
    assertProblem(await api.get("/polls/nope/results"), 404);
    const getVotes = await api.get(`/polls/${id}/votes`);
    assertProblem(getVotes, 405);
    assert.equal(getVotes.headers.get("allow"), "POST");
    const postResults = await api.post(`/polls/${id}/results`, {});
    assertProblem(postResults, 405);
    assert.equal(postResults.headers.get("allow"), "GET");
    assertProblem(await api.get(`/polls/${id}/other`), 404);
  });
});

describe("Idempotency-Key", () => {
  test("a retried vote gets the same answer, marked as replayed, and counts once", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    const first = await vote(api, id, "ana", 2, { "idempotency-key": "vote-123" });
    assert.equal(first.status, 201);
    assert.equal(first.headers.get("idempotent-replayed"), null);
    const retry = await vote(api, id, "ana", 2, { "idempotency-key": "vote-123" });
    assert.equal(retry.status, 201, "a retry is not a second vote");
    assert.deepEqual(retry.body, first.body);
    assert.equal(retry.headers.get("idempotent-replayed"), "true");
    assert.equal((await api.get(`/polls/${id}/results`)).body.total, 1);
  });

  test("a retried POST /polls does not create a second poll", async () => {
    const api = await startApp();
    const first = await api.post("/polls", pollBody(), { "idempotency-key": "create-1" });
    const retry = await api.post("/polls", pollBody(), { "idempotency-key": "create-1" });
    assert.equal(retry.status, 201);
    assert.equal(retry.body.id, first.body.id);
    assert.equal(retry.headers.get("location"), first.headers.get("location"));
    const other = await api.post("/polls", pollBody(), { "idempotency-key": "create-2" });
    assert.notEqual(other.body.id, first.body.id);
  });

  test("the same key with a different request is a 422", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    await vote(api, id, "ana", 1, { "idempotency-key": "k-1" });
    assertProblem(await vote(api, id, "ana", 2, { "idempotency-key": "k-1" }), 422);
    assertProblem(await vote(api, id, "bia", 1, { "idempotency-key": "k-1" }), 422);
  });

  test("an error is replayed too, and a vote without a key is never replayed", async () => {
    const api = await startApp();
    const id = await newPoll(api);
    assertProblem(await vote(api, id, "ana", 9, { "idempotency-key": "bad" }), 422);
    const again = await vote(api, id, "ana", 9, { "idempotency-key": "bad" });
    assertProblem(again, 422);
    assert.equal(again.headers.get("idempotent-replayed"), "true");
    await vote(api, id, "bia", 1);
    assertProblem(await vote(api, id, "bia", 1), 409);
  });
});
