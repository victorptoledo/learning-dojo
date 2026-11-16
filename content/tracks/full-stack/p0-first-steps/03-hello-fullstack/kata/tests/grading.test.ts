import assert from "node:assert/strict";
import type { AddressInfo } from "node:net";
import { test, type TestContext } from "node:test";
import { escapeHtml, fetchEntries, postEntry, renderEntries } from "../client.ts";
import { createGuestbookServer } from "../server.ts";

async function start(t: TestContext): Promise<string> {
  const server = createGuestbookServer();
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => server.close());
  return `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
}

function post(base: string, body: string): Promise<Response> {
  return fetch(`${base}/api/entries`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
  });
}

test("escapes the five HTML special characters", () => {
  assert.equal(
    escapeHtml(`<a href="x">Tom & 'Jerry'</a>`),
    "&lt;a href=&quot;x&quot;&gt;Tom &amp; &#39;Jerry&#39;&lt;/a&gt;",
  );
  assert.equal(
    renderEntries([
      { id: 1, name: "<i>Eve</i>", message: "a & b" },
      { id: 2, name: "Bo", message: "ok" },
    ]),
    "<li><strong>&lt;i&gt;Eve&lt;/i&gt;</strong>: a &amp; b</li><li><strong>Bo</strong>: ok</li>",
  );
  assert.equal(renderEntries([]), "");
});

test("POST creates an entry with 201, a Location and trimmed fields", async (t) => {
  const base = await start(t);
  const response = await post(base, JSON.stringify({ name: "  Ada ", message: " Hi there " }));
  assert.equal(response.status, 201);
  assert.equal(response.headers.get("location"), "/api/entries/1");
  assert.match(response.headers.get("content-type") ?? "", /application\/json/);
  assert.deepEqual(await response.json(), { id: 1, name: "Ada", message: "Hi there" });
});

test("entries are listed oldest first with increasing ids", async (t) => {
  const base = await start(t);
  await (await post(base, JSON.stringify({ name: "Ada", message: "first" }))).text();
  await (await post(base, JSON.stringify({ name: "Linus", message: "second" }))).text();
  const body = await (await fetch(`${base}/api/entries`)).json();
  assert.deepEqual(body, {
    entries: [
      { id: 1, name: "Ada", message: "first" },
      { id: 2, name: "Linus", message: "second" },
    ],
  });
});

test("a body that is not JSON is a 400 with an error", async (t) => {
  const base = await start(t);
  const response = await post(base, "name=Ada");
  assert.equal(response.status, 400);
  const body = (await response.json()) as { error?: unknown };
  assert.equal(typeof body.error, "string");
});

test("missing, blank or non-string fields are a 400 and nothing is stored", async (t) => {
  const base = await start(t);
  for (const body of [{ name: "Ada" }, { name: "   ", message: "x" }, { name: "Ada", message: 42 }, null]) {
    const response = await post(base, JSON.stringify(body));
    assert.equal(response.status, 400, `for ${JSON.stringify(body)}`);
    await response.text();
  }
  assert.deepEqual(await (await fetch(`${base}/api/entries`)).json(), { entries: [] });
});

test("GET / is an HTML page that lists the entries, escaped", async (t) => {
  const base = await start(t);
  await (await post(base, JSON.stringify({ name: "Eve", message: "<script>alert(1)</script>" }))).text();
  const response = await fetch(`${base}/`);
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html; ?charset=utf-8$/i);
  const html = await response.text();
  assert.match(
    html,
    /<ul id="entries">\s*<li><strong>Eve<\/strong>: &lt;script&gt;alert\(1\)&lt;\/script&gt;<\/li>\s*<\/ul>/,
  );
  assert.doesNotMatch(html, /<script>alert/);
});

test("other methods on /api/entries are 405 with Allow", async (t) => {
  const base = await start(t);
  const response = await fetch(`${base}/api/entries`, { method: "DELETE" });
  assert.equal(response.status, 405);
  assert.equal(response.headers.get("allow"), "GET, POST");
  await response.text();
});

test("unknown paths are a JSON 404", async (t) => {
  const base = await start(t);
  const response = await fetch(`${base}/nope`);
  assert.equal(response.status, 404);
  assert.deepEqual(await response.json(), { error: "not found" });
});

test("the client posts and fetches through the real server", async (t) => {
  const base = await start(t);
  const created = await postEntry(base, { name: "Grace", message: "Hello" });
  assert.deepEqual(created, { id: 1, name: "Grace", message: "Hello" });
  assert.deepEqual(await fetchEntries(base), [created]);
});

test("the client sends JSON and throws with the status or the server's error", async (t) => {
  const base = await start(t);
  await assert.rejects(postEntry(base, { name: "", message: "x" }), (error: Error) => {
    assert.match(error.message, /name|message|blank|missing|must/i);
    return true;
  });
  const seen: RequestInit[] = [];
  const failing: typeof fetch = async (_url, init) => {
    seen.push(init ?? {});
    return new Response("oops", { status: 503 });
  };
  await assert.rejects(fetchEntries(base, failing), /503/);
  const posting: typeof fetch = async (_url, init) => {
    seen.push(init ?? {});
    return new Response(JSON.stringify({ id: 9, name: "A", message: "B" }), { status: 201 });
  };
  assert.deepEqual(await postEntry(base, { name: "A", message: "B" }, posting), { id: 9, name: "A", message: "B" });
  const last = seen.at(-1);
  assert.equal(last?.method, "POST");
  assert.equal(new Headers(last?.headers).get("content-type"), "application/json");
  assert.deepEqual(JSON.parse(String(last?.body)), { name: "A", message: "B" });
});
