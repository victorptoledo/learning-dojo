import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { assertProblem, errorPaths, pollBody, startApp, type Api } from "./api.ts";

/** Creates polls "Q1".."Qn", one second apart, and returns their ids in creation order. */
async function seed(api: Api, count: number, overrides: (i: number) => Record<string, unknown> = () => ({})): Promise<string[]> {
  const ids: string[] = [];
  for (let i = 1; i <= count; i++) {
    const reply = await api.post("/polls", pollBody({ question: `Q${i}`, ...overrides(i) }));
    assert.equal(reply.status, 201, reply.text);
    ids.push(reply.body.id);
    api.advance(1000);
  }
  return ids;
}

const questions = (items: { question: string }[]) => items.map((item) => item.question);

/** Follows nextCursor until the end and returns every question seen. */
async function walk(api: Api, query: string): Promise<string[]> {
  const seen: string[] = [];
  let cursor: string | null = null;
  for (let page = 0; page < 50; page++) {
    const reply = await api.get(`/polls?${query}${cursor === null ? "" : `&cursor=${encodeURIComponent(cursor)}`}`);
    assert.equal(reply.status, 200, reply.text);
    seen.push(...questions(reply.body.items));
    cursor = reply.body.nextCursor;
    if (cursor === null) return seen;
  }
  throw new Error("the pages never end");
}

describe("GET /polls", () => {
  test("newest first, 20 by default, with a nextCursor while there are more", async () => {
    const api = await startApp();
    await seed(api, 23);
    const first = await api.get("/polls");
    assert.equal(first.status, 200);
    assert.equal(first.body.items.length, 20);
    assert.equal(first.body.items[0].question, "Q23");
    assert.equal(first.body.items[19].question, "Q4");
    assert.deepEqual(Object.keys(first.body.items[0]).sort(), ["closesAt", "createdAt", "id", "options", "question", "status"]);
    assert.equal(typeof first.body.nextCursor, "string");
    const second = await api.get(`/polls?cursor=${encodeURIComponent(first.body.nextCursor)}`);
    assert.deepEqual(questions(second.body.items), ["Q3", "Q2", "Q1"]);
    assert.equal(second.body.nextCursor, null);
  });

  test("an empty list is an empty page", async () => {
    const api = await startApp();
    assert.deepEqual((await api.get("/polls")).body, { items: [], nextCursor: null });
  });

  test("limit sets the page size; walking the pages sees every poll once", async () => {
    const api = await startApp();
    await seed(api, 7);
    assert.deepEqual(await walk(api, "limit=3"), ["Q7", "Q6", "Q5", "Q4", "Q3", "Q2", "Q1"]);
    assert.deepEqual(await walk(api, "limit=7"), ["Q7", "Q6", "Q5", "Q4", "Q3", "Q2", "Q1"]);
    const exact = await api.get("/polls?limit=7");
    assert.equal(exact.body.nextCursor, null, "no next page when nothing is left");
  });

  test("polls created between two pages neither repeat nor hide older ones", async () => {
    const api = await startApp();
    await seed(api, 5);
    const first = await api.get("/polls?limit=2");
    assert.deepEqual(questions(first.body.items), ["Q5", "Q4"]);
    for (const question of ["N1", "N2", "N3"]) {
      await api.post("/polls", pollBody({ question }));
      api.advance(1000);
    }
    const second = await api.get(`/polls?limit=2&cursor=${encodeURIComponent(first.body.nextCursor)}`);
    assert.deepEqual(questions(second.body.items), ["Q3", "Q2"]);
  });

  test("polls created in the same millisecond still page without gaps or repeats", async () => {
    const api = await startApp();
    const ids: string[] = [];
    for (let i = 0; i < 9; i++) {
      ids.push((await api.post("/polls", pollBody({ question: `Same ${i}` }))).body.id);
    }
    const seen = await walk(api, "limit=2");
    assert.equal(seen.length, 9);
    assert.equal(new Set(seen).size, 9);
  });

  test("status=open and status=closed filter by the clock, and the cursor keeps the filter", async () => {
    const api = await startApp();
    // Odd polls close two seconds after they are created.
    await seed(api, 6, (i) => (i % 2 === 1 ? { closesAt: new Date(api.now().getTime() + 2000).toISOString() } : {}));
    api.advance(5000);
    assert.deepEqual(await walk(api, "status=open&limit=2"), ["Q6", "Q4", "Q2"]);
    assert.deepEqual(await walk(api, "status=closed&limit=2"), ["Q5", "Q3", "Q1"]);
  });

  test("a Link header points to the next page with the same limit and filter", async () => {
    const api = await startApp();
    await seed(api, 3);
    const first = await api.get("/polls?limit=2&status=open");
    const link = first.headers.get("link") ?? "";
    const match = /^<([^>]+)>; rel="next"$/.exec(link);
    assert.ok(match, `expected a Link with rel="next", got ${JSON.stringify(link)}`);
    const next = new URL(match[1] ?? "", "http://x");
    assert.equal(next.pathname, "/polls");
    assert.equal(next.searchParams.get("limit"), "2");
    assert.equal(next.searchParams.get("status"), "open");
    assert.equal(next.searchParams.get("cursor"), first.body.nextCursor);
    const second = await api.get(match[1] ?? "");
    assert.deepEqual(questions(second.body.items), ["Q1"]);
    assert.equal(second.headers.get("link"), null);
  });

  test("400 problem with errors for a bad limit, status or cursor", async () => {
    const api = await startApp();
    await seed(api, 1);
    const cases: [string, string[]][] = [
      ["limit=0", ["limit"]],
      ["limit=101", ["limit"]],
      ["limit=ten", ["limit"]],
      ["limit=2.5", ["limit"]],
      ["status=maybe", ["status"]],
      ["cursor=not-a-cursor", ["cursor"]],
      ["limit=-1&status=x&cursor=%25%25", ["cursor", "limit", "status"]],
    ];
    for (const [query, paths] of cases) {
      const reply = await api.get(`/polls?${query}`);
      assertProblem(reply, 400);
      assert.deepEqual(errorPaths(reply), paths, query);
    }
    assert.equal((await api.get("/polls?limit=100")).status, 200);
  });

  test("405 on /polls now allows GET and POST", async () => {
    const api = await startApp();
    const reply = await api.call("DELETE", "/polls");
    assertProblem(reply, 405);
    assert.equal(reply.headers.get("allow"), "GET, POST");
  });
});
