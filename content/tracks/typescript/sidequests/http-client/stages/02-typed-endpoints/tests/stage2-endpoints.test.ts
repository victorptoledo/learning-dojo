import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { createApiClient, defineApi } from "../client.ts";
import { json, withServer } from "./server.ts";

type User = { id: number; name: string };
type NewUser = { name: string; admin?: boolean };

function parseUser(value: unknown): User {
  if (typeof value === "object" && value !== null && "id" in value && "name" in value) {
    const { id, name } = value;
    if (typeof id === "number" && typeof name === "string") {
      return { id, name };
    }
  }
  throw new Error("not a user");
}

const parseUsers = (value: unknown): User[] => {
  if (!Array.isArray(value)) {
    throw new Error("not a list");
  }
  return value.map(parseUser);
};

const api = defineApi({
  listUsers: { method: "GET", path: "/users", decode: parseUsers },
  getUser: { method: "GET", path: "/users/:id", decode: parseUser },
  createUser: {
    method: "POST",
    path: "/users",
    encode: (user: NewUser) => ({ full_name: user.name, admin: user.admin ?? false }),
    decode: parseUser,
  },
  getPost: { method: "GET", path: "/users/:userId/posts/:postId", decode: (json: unknown) => json },
  removeUser: { method: "DELETE", path: "/users/:id", decode: () => true },
});

describe("defineApi", () => {
  test("returns the api it gets", () => {
    const plain = { ping: { method: "GET" as const, path: "/ping", decode: () => "pong" } };
    assert.equal(defineApi(plain), plain);
  });
});

describe("createApiClient", () => {
  test("has one method per endpoint", () => {
    const client = createApiClient(api, { baseUrl: "http://127.0.0.1:1" });
    for (const name of ["listUsers", "getUser", "createUser", "getPost", "removeUser"]) {
      assert.equal(typeof (client as Record<string, unknown>)[name], "function", name);
    }
  });

  test("a call without arguments sends the method and path", async () => {
    await withServer((_, response) => json(response, 200, [{ id: 1, name: "Ana" }]), async (server) => {
      const client = createApiClient(api, { baseUrl: server.url });
      assert.deepEqual(await client.listUsers(), { ok: true, value: [{ id: 1, name: "Ana" }] });
      assert.deepEqual(server.seen.map((seen) => `${seen.method} ${seen.url}`), ["GET /users"]);
    });
  });

  test("fills and encodes the path parameters", async () => {
    await withServer((_, response) => json(response, 200, { ok: 1 }), async (server) => {
      const client = createApiClient(api, { baseUrl: `${server.url}/v1/` });
      await client.getUser({ params: { id: "42" } });
      await client.getPost({ params: { userId: "a b/c", postId: "ç?#" } });
      assert.deepEqual(server.seen.map((seen) => seen.url), [
        "/v1/users/42",
        "/v1/users/a%20b%2Fc/posts/%C3%A7%3F%23",
      ]);
    });
  });

  test("sends what encode returns as the JSON body", async () => {
    await withServer((_, response) => json(response, 201, { id: 9, name: "Bia" }), async (server) => {
      const client = createApiClient(api, { baseUrl: server.url });
      const result = await client.createUser({ body: { name: "Bia" } });
      assert.deepEqual(result, { ok: true, value: { id: 9, name: "Bia" } });
      const [seen] = server.seen;
      assert.equal(seen?.method, "POST");
      assert.equal(seen?.headers["content-type"], "application/json");
      assert.deepEqual(JSON.parse(seen?.body ?? ""), { full_name: "Bia", admin: false });
    });
  });

  test("endpoints without encode send no body", async () => {
    await withServer((_, response) => {
      response.statusCode = 204;
      response.end();
    }, async (server) => {
      const client = createApiClient(api, { baseUrl: server.url });
      assert.deepEqual(await client.removeUser({ params: { id: "3" } }), { ok: true, value: true });
      assert.equal(server.seen[0]?.body, "");
      assert.equal(server.seen[0]?.method, "DELETE");
    });
  });

  test("passes the query along", async () => {
    await withServer((_, response) => json(response, 200, []), async (server) => {
      const client = createApiClient(api, { baseUrl: server.url });
      await client.listUsers({ query: { page: 2, name: "a&b" } });
      assert.equal(server.seen[0]?.url, "/users?page=2&name=a%26b");
    });
  });

  test("errors come back as results, like request", async () => {
    await withServer((_, response) => json(response, 404, { error: "no such user" }), async (server) => {
      const client = createApiClient(api, { baseUrl: server.url });
      assert.deepEqual(await client.getUser({ params: { id: "0" } }), {
        ok: false,
        error: { kind: "status", status: 404, body: '{"error":"no such user"}' },
      });
    });
    await withServer((_, response) => json(response, 200, { id: "x" }), async (server) => {
      const client = createApiClient(api, { baseUrl: server.url });
      assert.deepEqual(await client.getUser({ params: { id: "1" } }), {
        ok: false,
        error: { kind: "decode", message: "not a user" },
      });
    });
  });

  test("each client keeps its own base URL", async () => {
    await withServer((_, response) => json(response, 200, []), async (first) => {
      await withServer((_, response) => json(response, 200, []), async (second) => {
        await createApiClient(api, { baseUrl: first.url }).listUsers();
        await createApiClient(api, { baseUrl: second.url }).listUsers();
        await createApiClient(api, { baseUrl: second.url }).listUsers();
        assert.equal(first.seen.length, 1);
        assert.equal(second.seen.length, 2);
      });
    });
  });
});
