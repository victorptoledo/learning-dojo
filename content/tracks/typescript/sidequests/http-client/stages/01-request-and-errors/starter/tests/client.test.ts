// Your own tests: `npm test` runs every tests/*.test.ts. Add more as you go.
import assert from "node:assert/strict";
import { test } from "node:test";
import { createClient } from "../client.ts";

test("a dead server is a network error, not an exception", async () => {
  const client = createClient({ baseUrl: "http://127.0.0.1:1" });
  const result = await client.request({ method: "GET", path: "/", decode: (json) => json });
  assert.equal(result.ok, false);
});
