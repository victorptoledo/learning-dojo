import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { createClient } from "../client.ts";
import { json, withServer } from "./server.ts";

const any = (value: unknown) => value;

type User = { id: number; name: string };
function parseUser(value: unknown): User {
  if (typeof value === "object" && value !== null && "id" in value && "name" in value) {
    const { id, name } = value;
    if (typeof id === "number" && typeof name === "string") {
      return { id, name };
    }
  }
  throw new Error("not a user");
}

describe("the request", () => {
  test("joins baseUrl and path without doubling the slash", async () => {
    await withServer((_, response) => json(response, 200, null), async (server) => {
      await createClient({ baseUrl: `${server.url}/api/` }).request({ method: "GET", path: "/users", decode: any });
      await createClient({ baseUrl: `${server.url}/api` }).request({ method: "GET", path: "/users", decode: any });
      assert.deepEqual(server.seen.map((seen) => seen.url), ["/api/users", "/api/users"]);
    });
  });

  test("encodes the query in key order and skips undefined", async () => {
    await withServer((_, response) => json(response, 200, null), async (server) => {
      const client = createClient({ baseUrl: server.url });
      await client.request({
        method: "GET",
        path: "/search",
        query: { q: "café & bar", page: 2, exact: true, skip: undefined, tag: "a/b" },
        decode: any,
      });
      await client.request({ method: "GET", path: "/search", query: {}, decode: any });
      await client.request({ method: "GET", path: "/search", query: { only: undefined }, decode: any });
      assert.deepEqual(server.seen.map((seen) => seen.url), [
        "/search?q=caf%C3%A9+%26+bar&page=2&exact=true&tag=a%2Fb",
        "/search",
        "/search",
      ]);
    });
  });

  test("sends the method, accept, and a JSON body with its content type", async () => {
    await withServer((_, response) => json(response, 201, { id: 1, name: "Ana" }), async (server) => {
      const client = createClient({ baseUrl: server.url });
      const result = await client.request({ method: "POST", path: "/users", body: { name: "Ana" }, decode: parseUser });
      assert.deepEqual(result, { ok: true, value: { id: 1, name: "Ana" } });
      const [seen] = server.seen;
      assert.equal(seen?.method, "POST");
      assert.equal(seen?.headers["accept"], "application/json");
      assert.equal(seen?.headers["content-type"], "application/json");
      assert.deepEqual(JSON.parse(seen?.body ?? ""), { name: "Ana" });
    });
  });

  test("without a body, sends no body and no content type", async () => {
    await withServer((_, response) => json(response, 200, null), async (server) => {
      await createClient({ baseUrl: server.url }).request({ method: "DELETE", path: "/users/1", decode: any });
      const [seen] = server.seen;
      assert.equal(seen?.method, "DELETE");
      assert.equal(seen?.body, "");
      assert.equal(seen?.headers["content-type"], undefined);
    });
  });

  test("every method goes out as written", async () => {
    await withServer((_, response) => json(response, 200, null), async (server) => {
      const client = createClient({ baseUrl: server.url });
      for (const method of ["GET", "POST", "PUT", "PATCH", "DELETE"] as const) {
        await client.request({ method, path: "/x", decode: any });
      }
      assert.deepEqual(server.seen.map((seen) => seen.method), ["GET", "POST", "PUT", "PATCH", "DELETE"]);
    });
  });
});

describe("the answer", () => {
  test("a 2xx JSON body goes through decode", async () => {
    await withServer((_, response) => json(response, 200, { id: 7, name: "Bia" }), async (server) => {
      const result = await createClient({ baseUrl: server.url }).request({ method: "GET", path: "/users/7", decode: parseUser });
      assert.deepEqual(result, { ok: true, value: { id: 7, name: "Bia" } });
    });
  });

  test("an empty 2xx body decodes undefined", async () => {
    await withServer((_, response) => {
      response.statusCode = 204;
      response.end();
    }, async (server) => {
      const seen: unknown[] = [];
      const result = await createClient({ baseUrl: server.url }).request({
        method: "DELETE",
        path: "/users/7",
        decode: (value) => {
          seen.push(value);
          return "deleted";
        },
      });
      assert.deepEqual(result, { ok: true, value: "deleted" });
      assert.deepEqual(seen, [undefined]);
    });
  });

  test("a status outside 2xx is a status error with the body as text", async () => {
    for (const status of [400, 404, 409, 500, 503]) {
      await withServer((_, response) => {
        response.writeHead(status, { "content-type": "text/plain" });
        response.end(`nope ${status}`);
      }, async (server) => {
        const result = await createClient({ baseUrl: server.url }).request({ method: "GET", path: "/", decode: any });
        assert.deepEqual(result, { ok: false, error: { kind: "status", status, body: `nope ${status}` } });
      });
    }
  });

  test("invalid JSON is a decode error", async () => {
    await withServer((_, response) => {
      response.writeHead(200, { "content-type": "application/json" });
      response.end("{ not json");
    }, async (server) => {
      const result = await createClient({ baseUrl: server.url }).request({ method: "GET", path: "/", decode: any });
      assert.equal(result.ok, false);
      if (!result.ok) {
        assert.equal(result.error.kind, "decode");
      }
    });
  });

  test("decode throwing is a decode error with its message", async () => {
    await withServer((_, response) => json(response, 200, { id: "7" }), async (server) => {
      const result = await createClient({ baseUrl: server.url }).request({ method: "GET", path: "/", decode: parseUser });
      assert.deepEqual(result, { ok: false, error: { kind: "decode", message: "not a user" } });
    });
  });
});

describe("network failures", () => {
  test("a closed port is a network error, never an exception", async () => {
    const result = await createClient({ baseUrl: "http://127.0.0.1:1" }).request({ method: "GET", path: "/", decode: any });
    assert.equal(result.ok, false);
    if (!result.ok) {
      assert.equal(result.error.kind, "network");
      assert.ok(result.error.kind === "network" && result.error.message.length > 0);
    }
  });

  test("a bad base URL is a network error too", async () => {
    const result = await createClient({ baseUrl: "not a url" }).request({ method: "GET", path: "/", decode: any });
    assert.equal(result.ok, false);
    if (!result.ok) {
      assert.equal(result.error.kind, "network");
    }
  });

  test("a server that hangs up mid-answer is a network error", async () => {
    await withServer((request, response) => {
      void request;
      response.writeHead(200, { "content-length": "100" });
      response.write("{");
      response.socket?.destroy();
    }, async (server) => {
      const result = await createClient({ baseUrl: server.url }).request({ method: "GET", path: "/", decode: any });
      assert.equal(result.ok, false);
      if (!result.ok) {
        assert.equal(result.error.kind, "network");
      }
    });
  });
});
