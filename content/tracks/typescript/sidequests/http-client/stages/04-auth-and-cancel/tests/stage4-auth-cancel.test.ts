import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { createApiClient, createClient, defineApi } from "../client.ts";
import { json, sleep, withServer } from "./server.ts";

const any = (value: unknown) => value;

describe("headers", () => {
  test("are sent on every request and can replace the client's own", async () => {
    await withServer((_, response) => json(response, 200, null), async (server) => {
      const client = createClient({ baseUrl: server.url, headers: { "x-api-version": "2", accept: "application/vnd.dojo+json" } });
      await client.request({ method: "GET", path: "/a", decode: any });
      await client.request({ method: "POST", path: "/b", body: 1, decode: any });
      for (const seen of server.seen) {
        assert.equal(seen.headers["x-api-version"], "2");
        assert.equal(seen.headers["accept"], "application/vnd.dojo+json");
      }
    });
  });
});

describe("auth", () => {
  test("sends the token as a bearer header", async () => {
    await withServer((_, response) => json(response, 200, null), async (server) => {
      await createClient({ baseUrl: server.url, auth: () => "abc" }).request({ method: "GET", path: "/", decode: any });
      assert.equal(server.seen[0]?.headers["authorization"], "Bearer abc");
    });
  });

  test("wins over an authorization in headers", async () => {
    await withServer((_, response) => json(response, 200, null), async (server) => {
      const client = createClient({ baseUrl: server.url, headers: { authorization: "Basic old" }, auth: () => "new" });
      await client.request({ method: "GET", path: "/", decode: any });
      assert.equal(server.seen[0]?.headers["authorization"], "Bearer new");
    });
  });

  test("is asked again before every attempt", async () => {
    await withServer((_, response, count) => (count < 3 ? json(response, 503, null) : json(response, 200, "ok")), async (server) => {
      let calls = 0;
      const client = createClient({
        baseUrl: server.url,
        retries: 2,
        retryDelayMs: 5,
        auth: async () => {
          calls += 1;
          await sleep(1);
          return `token-${calls}`;
        },
      });
      assert.deepEqual(await client.request({ method: "GET", path: "/", decode: any }), { ok: true, value: "ok" });
      assert.deepEqual(server.seen.map((seen) => seen.headers["authorization"]), ["Bearer token-1", "Bearer token-2", "Bearer token-3"]);
    });
  });

  test("failing is an auth error, and nothing is sent", async () => {
    await withServer((_, response) => json(response, 200, null), async (server) => {
      const sync = createClient({ baseUrl: server.url, retries: 2, auth: () => { throw new Error("no session"); } });
      assert.deepEqual(await sync.request({ method: "GET", path: "/", decode: any }), {
        ok: false,
        error: { kind: "auth", message: "no session" },
      });
      const rejected = createClient({ baseUrl: server.url, retries: 2, auth: () => Promise.reject(new Error("expired")) });
      assert.deepEqual(await rejected.request({ method: "GET", path: "/", decode: any }), {
        ok: false,
        error: { kind: "auth", message: "expired" },
      });
      assert.equal(server.seen.length, 0);
    });
  });
});

describe("Retry-After", () => {
  test("0 retries at once instead of the backoff", async () => {
    await withServer((_, response, count) => {
      if (count === 1) {
        response.writeHead(503, { "retry-after": "0" });
        response.end();
        return;
      }
      json(response, 200, "ok");
    }, async (server) => {
      const started = Date.now();
      const result = await createClient({ baseUrl: server.url, retries: 1, retryDelayMs: 5000 }).request({ method: "GET", path: "/", decode: any });
      assert.deepEqual(result, { ok: true, value: "ok" });
      assert.ok(Date.now() - started < 2000, `took ${Date.now() - started} ms`);
    });
  });

  test("a number of seconds replaces the backoff delay", async () => {
    await withServer((_, response, count) => {
      if (count === 1) {
        response.writeHead(429, { "retry-after": "1" });
        response.end();
        return;
      }
      json(response, 200, "ok");
    }, async (server) => {
      const started = Date.now();
      await createClient({ baseUrl: server.url, retries: 1, retryDelayMs: 1 }).request({ method: "GET", path: "/", decode: any });
      assert.ok(Date.now() - started >= 900, `took ${Date.now() - started} ms`);
    });
  });

  test("other values are ignored", async () => {
    await withServer((_, response, count) => {
      if (count === 1) {
        response.writeHead(503, { "retry-after": "Wed, 21 Oct 2015 07:28:00 GMT" });
        response.end();
        return;
      }
      json(response, 200, "ok");
    }, async (server) => {
      const started = Date.now();
      const result = await createClient({ baseUrl: server.url, retries: 1, retryDelayMs: 5 }).request({ method: "GET", path: "/", decode: any });
      assert.deepEqual(result, { ok: true, value: "ok" });
      assert.ok(Date.now() - started < 1000, `took ${Date.now() - started} ms`);
    });
  });
});

describe("cancelling", () => {
  test("an aborted signal sends nothing", async () => {
    await withServer((_, response) => json(response, 200, null), async (server) => {
      const result = await createClient({ baseUrl: server.url }).request({ method: "GET", path: "/", decode: any, signal: AbortSignal.abort() });
      assert.deepEqual(result, { ok: false, error: { kind: "aborted" } });
      assert.equal(server.seen.length, 0);
    });
  });

  test("aborting during an attempt stops it at once", async () => {
    await withServer(async (_, response) => {
      await sleep(3000);
      json(response, 200, null);
    }, async (server) => {
      const controller = new AbortController();
      setTimeout(() => controller.abort(), 100);
      const started = Date.now();
      const result = await createClient({ baseUrl: server.url, retries: 3, timeoutMs: 5000 }).request({
        method: "GET",
        path: "/",
        decode: any,
        signal: controller.signal,
      });
      assert.deepEqual(result, { ok: false, error: { kind: "aborted" } });
      assert.ok(Date.now() - started < 1500, `took ${Date.now() - started} ms`);
      assert.equal(server.seen.length, 1);
    });
  });

  test("aborting while waiting for a retry stops the wait", async () => {
    await withServer((_, response) => json(response, 503, null), async (server) => {
      const controller = new AbortController();
      setTimeout(() => controller.abort(), 200);
      const started = Date.now();
      const result = await createClient({ baseUrl: server.url, retries: 3, retryDelayMs: 5000 }).request({
        method: "GET",
        path: "/",
        decode: any,
        signal: controller.signal,
      });
      assert.deepEqual(result, { ok: false, error: { kind: "aborted" } });
      assert.ok(Date.now() - started < 1500, `took ${Date.now() - started} ms`);
      assert.equal(server.seen.length, 1);
    });
  });

  test("a timeout is still a timeout when the signal does not abort", async () => {
    await withServer(async (_, response) => {
      await sleep(2000);
      json(response, 200, null);
    }, async (server) => {
      const result = await createClient({ baseUrl: server.url, timeoutMs: 100 }).request({
        method: "GET",
        path: "/",
        decode: any,
        signal: new AbortController().signal,
      });
      assert.deepEqual(result, { ok: false, error: { kind: "timeout", ms: 100 } });
    });
  });

  test("the methods of createApiClient take a signal", async () => {
    const api = defineApi({ getUser: { method: "GET", path: "/users/:id", decode: any } });
    await withServer((_, response) => json(response, 200, null), async (server) => {
      const users = createApiClient(api, { baseUrl: server.url });
      const result = await users.getUser({ params: { id: "1" }, signal: AbortSignal.abort() });
      assert.deepEqual(result, { ok: false, error: { kind: "aborted" } });
      assert.equal(server.seen.length, 0);
    });
  });
});
