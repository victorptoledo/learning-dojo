import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { assertProblem, errorPaths, pollBody, START, startApp } from "./api.ts";

describe("POST /polls", () => {
  test("201 with the poll, a Location to it, and trimmed texts", async () => {
    const api = await startApp();
    const reply = await api.post("/polls", {
      question: "  Tabs or spaces? ",
      options: [" Tabs", "Spaces "],
      closesAt: "2026-03-02T12:00:00Z",
    });
    assert.equal(reply.status, 201, reply.text);
    assert.match(reply.headers.get("content-type") ?? "", /^application\/json/);
    const poll = reply.body;
    assert.equal(typeof poll.id, "string");
    assert.ok(poll.id.length > 0);
    assert.equal(reply.headers.get("location"), `/polls/${poll.id}`);
    assert.deepEqual(poll, {
      id: poll.id,
      question: "Tabs or spaces?",
      options: [
        { id: 1, text: "Tabs" },
        { id: 2, text: "Spaces" },
      ],
      status: "open",
      closesAt: "2026-03-02T12:00:00.000Z",
      createdAt: START.toISOString(),
    });
  });

  test("closesAt is optional and null when absent; every poll gets its own id", async () => {
    const api = await startApp();
    const first = await api.post("/polls", pollBody());
    const second = await api.post("/polls", pollBody());
    assert.equal(first.body.closesAt, null);
    assert.notEqual(first.body.id, second.body.id);
  });

  test("422 lists every invalid field at once", async () => {
    const api = await startApp();
    const reply = await api.post("/polls", {
      question: "   ",
      options: ["Yes", "yes ", "", 7, "x".repeat(101)],
      closesAt: "next tuesday",
      color: "red",
    });
    assertProblem(reply, 422);
    assert.deepEqual(errorPaths(reply), ["closesAt", "color", "options[1]", "options[2]", "options[3]", "options[4]", "question"]);
  });

  test("422 for missing fields, wrong types, the number of options, a past close and a body that is not an object", async () => {
    const api = await startApp();
    const cases: [unknown, string[]][] = [
      [{}, ["options", "question"]],
      [{ question: 42, options: "Tabs, Spaces" }, ["options", "question"]],
      [pollBody({ options: ["Only one"] }), ["options"]],
      [pollBody({ options: Array.from({ length: 11 }, (_, i) => `Option ${i}`) }), ["options"]],
      [pollBody({ question: "q".repeat(201) }), ["question"]],
      [pollBody({ closesAt: "2026-03-01T11:59:59Z" }), ["closesAt"]],
      [pollBody({ closesAt: START.toISOString() }), ["closesAt"]],
      [[], [""]],
      ["a poll", [""]],
      [null, [""]],
    ];
    for (const [body, paths] of cases) {
      const reply = await api.post("/polls", body);
      assertProblem(reply, 422);
      assert.deepEqual(errorPaths(reply), paths, `body ${JSON.stringify(body)}`);
    }
  });

  test("400 for a body that is not JSON, 415 for another content type", async () => {
    const api = await startApp();
    for (const body of ["{oops", "", "   "]) {
      assertProblem(await api.call("POST", "/polls", { body, headers: { "content-type": "application/json" } }), 400);
    }
    const asText = await api.call("POST", "/polls", { body: JSON.stringify(pollBody()), headers: { "content-type": "text/plain" } });
    assertProblem(asText, 415);
    const withCharset = await api.call("POST", "/polls", {
      body: JSON.stringify(pollBody()),
      headers: { "content-type": "application/json; charset=utf-8" },
    });
    assert.equal(withCharset.status, 201);
  });

  test("413 for a body over the limit, and the server keeps working", async () => {
    const api = await startApp({ maxBodyBytes: 1024 });
    const big = pollBody({ question: "Big?", options: ["a", "b"], padding: "x".repeat(4096) });
    assertProblem(await api.post("/polls", big), 413);
    assert.equal((await api.post("/polls", pollBody())).status, 201);
  });

  test("the default limit is 16 KiB", async () => {
    const api = await startApp();
    assertProblem(await api.post("/polls", pollBody({ padding: "x".repeat(17_000) })), 413);
    // Under the limit it is only invalid (an unknown field), not too large.
    assertProblem(await api.post("/polls", pollBody({ padding: "x".repeat(15_000) })), 422);
  });
});

describe("GET /polls/:id", () => {
  test("200 with the same poll; the status follows the clock", async () => {
    const api = await startApp();
    const created = (await api.post("/polls", pollBody({ closesAt: "2026-03-01T13:00:00Z" }))).body;
    const reply = await api.get(`/polls/${created.id}`);
    assert.equal(reply.status, 200);
    assert.deepEqual(reply.body, created);
    api.advance(60 * 60 * 1000);
    assert.equal((await api.get(`/polls/${created.id}`)).body.status, "closed");
  });

  test("404 problem for an unknown poll and an unknown route", async () => {
    const api = await startApp();
    assertProblem(await api.get("/polls/nope"), 404);
    assertProblem(await api.get("/nothing-here"), 404);
    assertProblem(await api.get("/"), 404);
  });

  test("405 problem with Allow for a method the route does not have", async () => {
    const api = await startApp();
    const id = (await api.post("/polls", pollBody())).body.id;
    const onList = await api.call("DELETE", "/polls");
    assertProblem(onList, 405);
    assert.ok((onList.headers.get("allow") ?? "").split(", ").includes("POST"));
    const onPoll = await api.call("DELETE", `/polls/${id}`);
    assertProblem(onPoll, 405);
    assert.ok((onPoll.headers.get("allow") ?? "").split(", ").includes("GET"));
  });

  test("each app keeps its own polls", async () => {
    const one = await startApp();
    const two = await startApp();
    const id = (await one.post("/polls", pollBody())).body.id;
    assert.equal((await two.get(`/polls/${id}`)).status, 404);
  });
});
