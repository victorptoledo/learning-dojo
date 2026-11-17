import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { createApiClient, createClient, defineApi, type Method } from "../client.ts";
import { json, sleep, withServer } from "./server.ts";

const any = (value: unknown) => value;

/** Answers `status` the first `failures` times, then 200 with `{ "n": <count> }`. */
const failing = (failures: number, status: number) => (_: unknown, response: Parameters<typeof json>[0], count: number) =>
  count <= failures ? json(response, status, { error: "try later" }) : json(response, 200, { n: count });

describe("timeouts", () => {
  test("an attempt slower than timeoutMs is a timeout error", async () => {
    await withServer(async (_, response) => {
      await sleep(1500);
      json(response, 200, null);
    }, async (server) => {
      const started = Date.now();
      const result = await createClient({ baseUrl: server.url, timeoutMs: 100 }).request({ method: "GET", path: "/", decode: any });
      assert.deepEqual(result, { ok: false, error: { kind: "timeout", ms: 100 } });
      assert.ok(Date.now() - started < 1000, `took ${Date.now() - started} ms`);
    });
  });

  test("the timeout covers the body too", async () => {
    await withServer(async (_, response) => {
      response.writeHead(200, { "content-type": "application/json" });
      response.write("[1,");
      await sleep(1500);
      response.end("2]");
    }, async (server) => {
      const result = await createClient({ baseUrl: server.url, timeoutMs: 150 }).request({ method: "GET", path: "/", decode: any });
      assert.deepEqual(result, { ok: false, error: { kind: "timeout", ms: 150 } });
    });
  });

  test("without timeoutMs the client waits", async () => {
    await withServer(async (_, response) => {
      await sleep(300);
      json(response, 200, "slow");
    }, async (server) => {
      const result = await createClient({ baseUrl: server.url }).request({ method: "GET", path: "/", decode: any });
      assert.deepEqual(result, { ok: true, value: "slow" });
    });
  });

  test("a fast answer is not affected", async () => {
    await withServer((_, response) => json(response, 200, 1), async (server) => {
      const result = await createClient({ baseUrl: server.url, timeoutMs: 2000 }).request({ method: "GET", path: "/", decode: any });
      assert.deepEqual(result, { ok: true, value: 1 });
    });
  });
});

describe("retries", () => {
  test("none by default", async () => {
    await withServer(failing(1, 503), async (server) => {
      const result = await createClient({ baseUrl: server.url }).request({ method: "GET", path: "/", decode: any });
      assert.equal(result.ok, false);
      assert.equal(server.seen.length, 1);
    });
  });

  for (const status of [429, 502, 503, 504]) {
    test(`a ${status} is retried`, async () => {
      await withServer(failing(2, status), async (server) => {
        const client = createClient({ baseUrl: server.url, retries: 2, retryDelayMs: 5 });
        assert.deepEqual(await client.request({ method: "GET", path: "/", decode: any }), { ok: true, value: { n: 3 } });
        assert.equal(server.seen.length, 3);
      });
    });
  }

  for (const status of [400, 401, 404, 409, 500]) {
    test(`a ${status} is not retried`, async () => {
      await withServer(failing(1, status), async (server) => {
        const client = createClient({ baseUrl: server.url, retries: 3, retryDelayMs: 5 });
        const result = await client.request({ method: "GET", path: "/", decode: any });
        assert.equal(result.ok, false);
        assert.equal(server.seen.length, 1);
      });
    });
  }

  test("when the attempts run out, the result is the last error", async () => {
    await withServer((_, response, count) => json(response, 503, { attempt: count }), async (server) => {
      const client = createClient({ baseUrl: server.url, retries: 2, retryDelayMs: 5 });
      assert.deepEqual(await client.request({ method: "GET", path: "/", decode: any }), {
        ok: false,
        error: { kind: "status", status: 503, body: '{"attempt":3}' },
      });
      assert.equal(server.seen.length, 3);
    });
  });

  test("a network error is retried", async () => {
    await withServer((_, response, count) => {
      if (count === 1) {
        response.socket?.destroy();
        return;
      }
      json(response, 200, "back");
    }, async (server) => {
      const client = createClient({ baseUrl: server.url, retries: 1, retryDelayMs: 5 });
      assert.deepEqual(await client.request({ method: "GET", path: "/", decode: any }), { ok: true, value: "back" });
      assert.equal(server.seen.length, 2);
    });
  });

  test("a timeout is retried", async () => {
    await withServer(async (_, response, count) => {
      if (count === 1) {
        await sleep(1500);
      }
      json(response, 200, count);
    }, async (server) => {
      const client = createClient({ baseUrl: server.url, retries: 1, retryDelayMs: 5, timeoutMs: 150 });
      assert.deepEqual(await client.request({ method: "GET", path: "/", decode: any }), { ok: true, value: 2 });
    });
  });

  test("a decode error is not retried", async () => {
    await withServer((_, response) => json(response, 200, "x"), async (server) => {
      const client = createClient({ baseUrl: server.url, retries: 3, retryDelayMs: 5 });
      const result = await client.request({
        method: "GET",
        path: "/",
        decode: () => {
          throw new Error("bad");
        },
      });
      assert.deepEqual(result, { ok: false, error: { kind: "decode", message: "bad" } });
      assert.equal(server.seen.length, 1);
    });
  });

  test("waits retryDelayMs, then twice as long each time", async () => {
    await withServer((_, response) => json(response, 503, null), async (server) => {
      const times: number[] = [];
      const client = createClient({ baseUrl: server.url, retries: 3, retryDelayMs: 100 });
      const original = server.seen.push.bind(server.seen);
      server.seen.push = (...items) => {
        times.push(Date.now());
        return original(...items);
      };
      await client.request({ method: "GET", path: "/", decode: any });
      assert.equal(times.length, 4);
      const gaps = times.slice(1).map((time, index) => time - (times[index] ?? 0));
      const [first = 0, second = 0, third = 0] = gaps;
      assert.ok(first >= 90 && second >= 190 && third >= 390, `gaps ${gaps.join(", ")}`);
      assert.ok(third < 1500, `gaps ${gaps.join(", ")}`);
    });
  });

  test("the default delay is 100 ms", async () => {
    await withServer(failing(1, 503), async (server) => {
      const started = Date.now();
      await createClient({ baseUrl: server.url, retries: 1 }).request({ method: "GET", path: "/", decode: any });
      assert.ok(Date.now() - started >= 90, `took ${Date.now() - started} ms`);
    });
  });
});

describe("what is safe to retry", () => {
  const cases: [Method, boolean | undefined, number][] = [
    ["GET", undefined, 2],
    ["PUT", undefined, 2],
    ["DELETE", undefined, 2],
    ["POST", undefined, 1],
    ["PATCH", undefined, 1],
    ["POST", true, 2],
    ["PATCH", true, 2],
    ["GET", false, 1],
  ];
  for (const [method, idempotent, attempts] of cases) {
    test(`${method}${idempotent === undefined ? "" : ` with idempotent: ${idempotent}`} makes ${attempts} attempts`, async () => {
      await withServer(failing(5, 503), async (server) => {
        const client = createClient({ baseUrl: server.url, retries: 1, retryDelayMs: 5 });
        await client.request(idempotent === undefined ? { method, path: "/", decode: any } : { method, path: "/", idempotent, decode: any });
        assert.equal(server.seen.length, attempts);
      });
    });
  }
});

describe("createApiClient", () => {
  test("uses the options and the endpoint's idempotent", async () => {
    const api = defineApi({
      pay: { method: "POST", path: "/payments", encode: (amount: number) => ({ amount }), decode: any, idempotent: true },
      order: { method: "POST", path: "/orders", encode: (item: string) => ({ item }), decode: any },
    });
    await withServer(failing(1, 503), async (server) => {
      const client = createApiClient(api, { baseUrl: server.url, retries: 2, retryDelayMs: 5 });
      assert.deepEqual(await client.pay({ body: 10 }), { ok: true, value: { n: 2 } });
    });
    await withServer(failing(1, 503), async (server) => {
      const client = createApiClient(api, { baseUrl: server.url, retries: 2, retryDelayMs: 5 });
      assert.equal((await client.order({ body: "book" })).ok, false);
      assert.equal(server.seen.length, 1);
    });
  });
});
