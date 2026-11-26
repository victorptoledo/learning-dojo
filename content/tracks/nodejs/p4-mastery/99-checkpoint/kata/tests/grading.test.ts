import assert from "node:assert/strict";
import { once } from "node:events";
import { createServer, request } from "node:http";
import type { AddressInfo } from "node:net";
import { afterEach, describe, test } from "node:test";
import { createGateway, currentRequestId, LruCache, requestContext, type Gateway, type GatewayOptions } from "../gateway.ts";

// ---- a fake product service the tests control -------------------------------------------

interface Behaviour {
  status?: number;
  body?: string;
  delayMs?: number;
  /** Never answers. */
  hang?: boolean;
  /** Answers when the test calls release(). */
  held?: boolean;
}

interface Upstream {
  url: string;
  calls: { id: string; requestId: string | undefined }[];
  behave: Map<string, Behaviour>;
  release(): void;
  close(): Promise<void>;
}

const product = (id: string, extra: Record<string, unknown> = {}) => JSON.stringify({ id, name: `Product ${id}`, price: 10, ...extra });

async function startUpstream(): Promise<Upstream> {
  const behave = new Map<string, Behaviour>();
  const calls: Upstream["calls"] = [];
  let waiting: (() => void)[] = [];
  const server = createServer((req, res) => {
    const id = /^\/products\/(.+)$/.exec(req.url ?? "")?.[1] ?? "";
    const header = req.headers["x-request-id"];
    calls.push({ id, requestId: typeof header === "string" ? header : undefined });
    const how = behave.get(id) ?? {};
    if (how.hang === true) {
      return;
    }
    const answer = () => {
      const status = how.status ?? 200;
      res.writeHead(status, { "content-type": "application/json" });
      res.end(how.body ?? (status === 200 ? product(id) : JSON.stringify({ error: `status ${status}` })));
    };
    if (how.held === true) {
      waiting.push(answer);
    } else {
      setTimeout(answer, how.delayMs ?? 0);
    }
  });
  server.listen(0, "127.0.0.1");
  await once(server, "listening");
  return {
    url: `http://127.0.0.1:${(server.address() as AddressInfo).port}`,
    calls,
    behave,
    release() {
      const now = waiting;
      waiting = [];
      for (const answer of now) answer();
    },
    async close() {
      server.closeAllConnections();
      server.close();
    },
  };
}

// ---- helpers ------------------------------------------------------------------------------

interface Started {
  gateway: Gateway;
  upstream: Upstream;
  base: string;
  lines: Record<string, unknown>[];
  clock: { now: number };
}

const cleanups: (() => Promise<void>)[] = [];

afterEach(async () => {
  for (const cleanup of cleanups.splice(0)) await cleanup();
});

async function start(options: Partial<GatewayOptions> = {}): Promise<Started> {
  const upstream = await startUpstream();
  cleanups.push(() => upstream.close()); // first, so it runs even when createGateway throws
  const lines: Record<string, unknown>[] = [];
  const clock = { now: 1_000_000 };
  const gateway = createGateway({
    upstream: upstream.url,
    ttlMs: 5_000,
    timeoutMs: 1_000,
    log: (line) => lines.push(JSON.parse(line) as Record<string, unknown>),
    now: () => clock.now,
    ...options,
  });
  gateway.server.listen(0, "127.0.0.1");
  await once(gateway.server, "listening");
  const base = `http://127.0.0.1:${(gateway.server.address() as AddressInfo).port}`;
  cleanups.unshift(async () => {
    upstream.release();
    gateway.server.closeAllConnections();
    gateway.server.close();
  });
  return { gateway, upstream, base, lines, clock };
}

async function get(url: string, headers: Record<string, string> = {}): Promise<{ status: number; headers: Headers; body: any }> {
  const response = await fetch(url, { headers });
  const text = await response.text();
  return { status: response.status, headers: response.headers, body: text === "" ? undefined : JSON.parse(text) };
}

/** A raw request on its own connection, so a test can keep it open while something else happens. */
function rawGet(url: string): Promise<{ status: number; connection: string | undefined; body: string }> {
  return new Promise((resolve, reject) => {
    const req = request(url, { agent: false }, (res) => {
      let body = "";
      res.setEncoding("utf8");
      res.on("data", (chunk: string) => (body += chunk));
      res.on("end", () => resolve({ status: res.statusCode ?? 0, connection: res.headers.connection, body }));
      res.on("error", reject);
    });
    req.on("error", reject);
    req.end();
  });
}

const until = async (check: () => boolean, ms = 2_000) => {
  const end = Date.now() + ms;
  while (!check()) {
    if (Date.now() > end) throw new Error("timed out waiting");
    await new Promise((resolve) => setTimeout(resolve, 5));
  }
};

// ---- tests ---------------------------------------------------------------------------------

describe("LruCache", () => {
  test("forgets the least recently used key first; get and set count as use", () => {
    const cache = new LruCache<number>(3);
    cache.set("a", 1);
    cache.set("b", 2);
    cache.set("c", 3);
    assert.equal(cache.get("a"), 1); // a is now the most recent
    cache.set("d", 4); // b goes
    assert.deepEqual(cache.keys(), ["c", "a", "d"]);
    cache.set("c", 30); // c is updated and becomes the most recent
    cache.set("e", 5); // a goes
    assert.deepEqual(cache.keys(), ["d", "c", "e"]);
    assert.equal(cache.size, 3);
    assert.equal(cache.get("b"), undefined);
    assert.equal(cache.get("c"), 30);
    assert.equal(cache.delete("c"), true);
    assert.equal(cache.size, 2);
  });

  test("refuses a size that is not a whole number of at least 1", () => {
    for (const size of [0, -1, 1.5, Number.NaN]) {
      assert.throws(() => new LruCache(size), RangeError);
    }
  });
});

describe("products", () => {
  test("a miss calls the upstream once, then the product is served from the cache", async () => {
    const { base, upstream, gateway } = await start();
    const first = await get(`${base}/products/pen`);
    assert.equal(first.status, 200);
    assert.equal(first.headers.get("x-cache"), "miss");
    assert.match(first.headers.get("content-type") ?? "", /application\/json/);
    assert.deepEqual(first.body, JSON.parse(product("pen")));
    const second = await get(`${base}/products/pen`);
    assert.equal(second.headers.get("x-cache"), "hit");
    assert.deepEqual(second.body, first.body);
    assert.equal(upstream.calls.length, 1);
    await until(() => gateway.metrics().inFlight === 0);
    assert.deepEqual(gateway.metrics(), { requests: 2, hits: 1, misses: 1, stale: 0, shed: 0, upstreamCalls: 1, inFlight: 0, cacheEntries: 1 });
  });

  test("a cached product is fresh for ttlMs, then it is fetched again", async () => {
    const { base, upstream, clock } = await start({ ttlMs: 5_000 });
    await get(`${base}/products/pen`);
    clock.now += 4_999;
    assert.equal((await get(`${base}/products/pen`)).headers.get("x-cache"), "hit");
    clock.now += 1;
    upstream.behave.set("pen", { body: product("pen", { price: 12 }) });
    const later = await get(`${base}/products/pen`);
    assert.equal(later.headers.get("x-cache"), "miss");
    assert.equal(later.body.price, 12);
    assert.equal(upstream.calls.length, 2);
  });

  test("concurrent misses for one product share a single upstream call", async () => {
    const { base, upstream, gateway } = await start();
    upstream.behave.set("lamp", { held: true });
    const answers = Promise.all(Array.from({ length: 10 }, () => get(`${base}/products/lamp`)));
    await until(() => gateway.metrics().inFlight === 10);
    upstream.release();
    const all = await answers;
    assert.deepEqual(
      all.map((each) => each.status),
      Array(10).fill(200),
    );
    assert.equal(upstream.calls.length, 1);
    // the next miss, once the first call is over, is a new call
    assert.equal((await get(`${base}/products/desk`)).status, 200);
    assert.equal(upstream.calls.length, 2);
  });

  test("404 from the upstream passes through and is not cached", async () => {
    const { base, upstream } = await start();
    upstream.behave.set("ghost", { status: 404 });
    assert.equal((await get(`${base}/products/ghost`)).status, 404);
    upstream.behave.delete("ghost");
    assert.equal((await get(`${base}/products/ghost`)).status, 200);
    assert.equal(upstream.calls.length, 2);
  });

  test("bad ids, wrong methods and unknown paths never reach the upstream", async () => {
    const { base, upstream } = await start();
    for (const id of ["UPPER", "a%2Fb", "x".repeat(41), "dots.dots", "%00", "%E0%A4%A"]) {
      assert.equal((await get(`${base}/products/${id}`)).status, 400, id);
    }
    const post = await fetch(`${base}/products/pen`, { method: "POST", body: "{}" });
    assert.equal(post.status, 405);
    assert.equal(post.headers.get("allow"), "GET");
    await post.text();
    assert.equal((await get(`${base}/products`)).status, 404);
    assert.equal((await get(`${base}/admin`)).status, 404);
    assert.equal(upstream.calls.length, 0);
  });
});

describe("when the upstream fails", () => {
  test("an error becomes 502 with the request id, and nothing is cached", async () => {
    const { base, upstream } = await start();
    upstream.behave.set("pen", { status: 500 });
    const answer = await get(`${base}/products/pen`);
    assert.equal(answer.status, 502);
    assert.equal(answer.body.error, "upstream unavailable");
    assert.equal(answer.body.requestId, answer.headers.get("x-request-id"));
    upstream.behave.set("pen", { body: "<html>not json</html>" });
    assert.equal((await get(`${base}/products/pen`)).status, 502, "a body that is not JSON is an upstream failure");
    upstream.behave.delete("pen");
    assert.equal((await get(`${base}/products/pen`)).headers.get("x-cache"), "miss");
  });

  test("a slow upstream becomes 504 after timeoutMs", async () => {
    const { base, upstream } = await start({ timeoutMs: 200 });
    upstream.behave.set("pen", { hang: true });
    const started = performance.now();
    const answer = await get(`${base}/products/pen`);
    const took = performance.now() - started;
    assert.equal(answer.status, 504);
    assert.equal(answer.body.error, "upstream timeout");
    assert.ok(took >= 150 && took < 1_500, `answered after ${Math.round(took)} ms`);
  });

  test("an expired product is served stale instead of an error", async () => {
    const { base, upstream, clock, gateway } = await start({ timeoutMs: 200 });
    await get(`${base}/products/pen`);
    clock.now += 60_000;
    upstream.behave.set("pen", { status: 503 });
    const stale = await get(`${base}/products/pen`);
    assert.equal(stale.status, 200);
    assert.equal(stale.headers.get("x-cache"), "stale");
    assert.deepEqual(stale.body, JSON.parse(product("pen")));
    upstream.behave.set("pen", { hang: true });
    assert.equal((await get(`${base}/products/pen`)).headers.get("x-cache"), "stale");
    assert.equal(gateway.metrics().stale, 2);
  });
});

describe("request context", () => {
  test("the request id is answered, forwarded to the upstream and safe", async () => {
    const { base, upstream } = await start();
    const mine = await get(`${base}/products/pen`, { "x-request-id": "order-42_retry-1" });
    assert.equal(mine.headers.get("x-request-id"), "order-42_retry-1");
    assert.equal(upstream.calls[0]!.requestId, "order-42_retry-1");
    for (const bad of ["two words", "x".repeat(65), "line\tbreak", "<script>"]) {
      const answer = await get(`${base}/products/item-${upstream.calls.length}`, { "x-request-id": bad });
      const id = answer.headers.get("x-request-id") ?? "";
      assert.match(id, /^[0-9a-f-]{36}$/, `a new uuid instead of ${JSON.stringify(bad)}`);
      assert.equal(upstream.calls.at(-1)!.requestId, id);
    }
  });

  test("currentRequestId reads the store, and is undefined outside a request", () => {
    assert.equal(currentRequestId(), undefined);
    requestContext.run({ requestId: "abc" }, () => {
      assert.equal(currentRequestId(), "abc");
    });
  });

  test("one JSON access log line per request, with the request id and the cache result", async () => {
    const { base, lines } = await start();
    await get(`${base}/products/pen`, { "x-request-id": "first" });
    await get(`${base}/products/pen`, { "x-request-id": "second" });
    await get(`${base}/products/BAD`, { "x-request-id": "third" });
    await until(() => lines.filter((line) => line.msg === "request").length === 3);
    const access = lines.filter((line) => line.msg === "request");
    assert.deepEqual(
      access.map(({ requestId, method, path, status, cache }) => ({ requestId, method, path, status, cache })),
      [
        { requestId: "first", method: "GET", path: "/products/pen", status: 200, cache: "miss" },
        { requestId: "second", method: "GET", path: "/products/pen", status: 200, cache: "hit" },
        { requestId: "third", method: "GET", path: "/products/BAD", status: 400, cache: undefined },
      ],
    );
    for (const line of access) {
      assert.equal(line.level, "info");
      assert.equal(typeof line.ms, "number");
      assert.ok(!Number.isNaN(Date.parse(String(line.time))));
    }
  });
});

describe("capacity", () => {
  test("sheds load with 503 and Retry-After beyond maxInFlight, and recovers", async () => {
    const { base, upstream, gateway } = await start({ maxInFlight: 2 });
    upstream.behave.set("a", { held: true });
    upstream.behave.set("b", { held: true });
    const slow = [get(`${base}/products/a`), get(`${base}/products/b`)];
    await until(() => gateway.metrics().inFlight === 2);
    const shed = await get(`${base}/products/c`);
    assert.equal(shed.status, 503);
    assert.equal(shed.headers.get("retry-after"), "1");
    upstream.release();
    assert.deepEqual(
      (await Promise.all(slow)).map((each) => each.status),
      [200, 200],
    );
    await until(() => gateway.metrics().inFlight === 0);
    assert.equal((await get(`${base}/products/c`)).status, 200);
    const metrics = (await get(`${base}/metrics`)).body;
    assert.equal(metrics.shed, 1);
    assert.equal(metrics.inFlight, 0);
    assert.equal(upstream.calls.filter((call) => call.id === "c").length, 1);
  });

  test("the cache keeps at most maxEntries products, least recently used out first", async () => {
    const { base, upstream, gateway } = await start({ maxEntries: 2 });
    for (const id of ["a", "b", "a", "c"]) {
      await get(`${base}/products/${id}`);
    }
    assert.equal(gateway.metrics().cacheEntries, 2);
    assert.equal((await get(`${base}/products/a`)).headers.get("x-cache"), "hit");
    assert.equal((await get(`${base}/products/b`)).headers.get("x-cache"), "miss");
    assert.equal(upstream.calls.length, 4);
  });
});

describe("shutdown", () => {
  test("readiness goes off, requests in progress finish with connection: close, and it resolves", async () => {
    const { base, upstream, gateway } = await start();
    assert.deepEqual((await get(`${base}/ready`)).body, { ready: true });
    upstream.behave.set("pen", { held: true });
    const inProgress = rawGet(`${base}/products/pen`);
    await until(() => upstream.calls.length === 1);
    let finished = false;
    const stopped = gateway.shutdown(5_000).then(() => {
      finished = true;
    });
    assert.equal(gateway.ready, false);
    assert.equal(gateway.shutdown(5_000), gateway.shutdown(5_000), "a second call returns the same promise");
    await new Promise((resolve) => setTimeout(resolve, 50));
    assert.equal(finished, false, "shutdown waits for the request in progress");
    upstream.release();
    const answer = await inProgress;
    assert.equal(answer.status, 200);
    assert.equal(answer.connection, "close");
    await stopped;
    await assert.rejects(rawGet(`${base}/ready`), "new connections are refused");
  });

  test("a request that never ends is cut after graceMs", async () => {
    const { base, upstream, gateway } = await start({ timeoutMs: 60_000 });
    upstream.behave.set("pen", { hang: true });
    const stuck = rawGet(`${base}/products/pen`).catch((error: unknown) => error);
    await until(() => upstream.calls.length === 1);
    const started = performance.now();
    await gateway.shutdown(300);
    const took = performance.now() - started;
    assert.ok(took >= 250 && took < 2_000, `shutdown took ${Math.round(took)} ms`);
    assert.ok((await stuck) instanceof Error, "the stuck client sees its connection closed");
  });
});
