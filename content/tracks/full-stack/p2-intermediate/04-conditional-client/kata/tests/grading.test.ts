import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import type { AddressInfo } from "node:net";
import { test, type TestContext } from "node:test";
import { CachingClient, ConflictError } from "../client.ts";
import { Catalog, createProductServer, etagOf, matches, type Product } from "../server.ts";

const MUG: Product = { id: 1, name: "Mug", priceCents: 1200 };

async function start(t: TestContext, catalog = new Catalog([MUG])): Promise<{ base: string; catalog: Catalog }> {
  const server = createProductServer(catalog);
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => server.close());
  return { base: `http://127.0.0.1:${(server.address() as AddressInfo).port}`, catalog };
}

function put(url: string, body: object, ifMatch?: string): Promise<Response> {
  return fetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...(ifMatch === undefined ? {} : { "If-Match": ifMatch }) },
    body: JSON.stringify(body),
  });
}

test("etagOf is the first 16 base64url characters of the SHA-256, quoted", () => {
  const body = JSON.stringify(MUG);
  const expected = `"${createHash("sha256").update(body).digest("base64url").slice(0, 16)}"`;
  assert.equal(etagOf(body), expected);
  assert.notEqual(etagOf(body), etagOf(JSON.stringify({ ...MUG, priceCents: 1300 })));
});

test("matches handles *, lists, spaces and weak tags", () => {
  assert.equal(matches("*", '"x"'), true);
  assert.equal(matches('"a", "b" , "c"', '"b"'), true);
  assert.equal(matches('W/"b"', '"b"'), true);
  assert.equal(matches('"a", W/"c"', '"c"'), true);
  assert.equal(matches('"a"', '"b"'), false);
  assert.equal(matches(undefined, '"b"'), false);
  assert.equal(matches('"bb"', '"b"'), false);
});

test("GET answers with the product, an ETag and Cache-Control: no-cache", async (t) => {
  const { base } = await start(t);
  const response = await fetch(`${base}/api/products/1`);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("etag"), etagOf(JSON.stringify(MUG)));
  assert.equal(response.headers.get("cache-control"), "no-cache");
  assert.deepEqual(await response.json(), MUG);
});

test("If-None-Match with the current ETag is a 304 with no body", async (t) => {
  const { base } = await start(t);
  const etag = etagOf(JSON.stringify(MUG));
  const response = await fetch(`${base}/api/products/1`, { headers: { "If-None-Match": `"old", ${etag}` } });
  assert.equal(response.status, 304);
  assert.equal(response.headers.get("etag"), etag);
  assert.equal(await response.text(), "");
  const stale = await fetch(`${base}/api/products/1`, { headers: { "If-None-Match": '"old"' } });
  assert.equal(stale.status, 200);
  await stale.text();
});

test("PUT without If-Match is 428 and changes nothing", async (t) => {
  const { base, catalog } = await start(t);
  const response = await put(`${base}/api/products/1`, { priceCents: 1 });
  assert.equal(response.status, 428);
  await response.text();
  assert.deepEqual(catalog.get(1), MUG);
});

test("PUT with a stale If-Match is 412 with the current product and ETag", async (t) => {
  const { base, catalog } = await start(t);
  catalog.update(1, { priceCents: 1500 });
  const current = catalog.get(1);
  const response = await put(`${base}/api/products/1`, { name: "Big mug" }, etagOf(JSON.stringify(MUG)));
  assert.equal(response.status, 412);
  assert.equal(response.headers.get("etag"), etagOf(JSON.stringify(current)));
  assert.deepEqual(await response.json(), current);
  assert.equal(catalog.get(1)?.name, "Mug");
});

test("PUT with the current If-Match updates and returns the new ETag", async (t) => {
  const { base, catalog } = await start(t);
  const response = await put(`${base}/api/products/1`, { priceCents: 990 }, etagOf(JSON.stringify(MUG)));
  assert.equal(response.status, 200);
  const updated = { ...MUG, priceCents: 990 };
  assert.deepEqual(await response.json(), updated);
  assert.equal(response.headers.get("etag"), etagOf(JSON.stringify(updated)));
  assert.deepEqual(catalog.get(1), updated);
});

test("unknown products are 404", async (t) => {
  const { base } = await start(t);
  const response = await fetch(`${base}/api/products/42`);
  assert.equal(response.status, 404);
  await response.text();
});

test("the client downloads once, then revalidates", async (t) => {
  const { base } = await start(t);
  const client = new CachingClient(base);
  assert.deepEqual(await client.get("/api/products/1"), MUG);
  assert.deepEqual(await client.get("/api/products/1"), MUG);
  assert.deepEqual(await client.get("/api/products/1"), MUG);
  assert.deepEqual(client.stats, { downloaded: 1, revalidated: 2 });
});

test("the client sees a change on the next revalidation", async (t) => {
  const { base, catalog } = await start(t);
  const client = new CachingClient(base);
  await client.get("/api/products/1");
  catalog.update(1, { name: "Travel mug" });
  assert.deepEqual(await client.get("/api/products/1"), { ...MUG, name: "Travel mug" });
  await client.get("/api/products/1");
  assert.deepEqual(client.stats, { downloaded: 2, revalidated: 1 });
});

test("the client updates with If-Match and keeps the new ETag", async (t) => {
  const { base } = await start(t);
  const client = new CachingClient(base);
  await client.get("/api/products/1");
  assert.deepEqual(await client.update("/api/products/1", { priceCents: 1100 }), { ...MUG, priceCents: 1100 });
  assert.deepEqual(await client.update("/api/products/1", { name: "Cup" }), { id: 1, name: "Cup", priceCents: 1100 });
  assert.deepEqual(await client.get("/api/products/1"), { id: 1, name: "Cup", priceCents: 1100 });
  assert.equal(client.stats.revalidated, 1);
});

test("two editors: the second gets a ConflictError with the winner's version", async (t) => {
  const { base, catalog } = await start(t);
  const alice = new CachingClient(base);
  const bob = new CachingClient(base);
  await alice.get("/api/products/1");
  await bob.get("/api/products/1");
  await alice.update("/api/products/1", { priceCents: 1300 });
  await assert.rejects(bob.update("/api/products/1", { priceCents: 1000 }), (error: unknown) => {
    assert.ok(error instanceof ConflictError);
    assert.deepEqual(error.current, { ...MUG, priceCents: 1300 });
    return true;
  });
  assert.equal(catalog.get(1)?.priceCents, 1300);
  assert.deepEqual(await bob.update("/api/products/1", { priceCents: 1000 }), { ...MUG, priceCents: 1000 });
});

test("the client refuses to update what it never loaded and reports other errors", async (t) => {
  const { base } = await start(t);
  const client = new CachingClient(base);
  await assert.rejects(client.update("/api/products/1", { name: "x" }), /get/);
  await assert.rejects(client.get("/api/products/9"), /404/);
});
