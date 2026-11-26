import assert from "node:assert/strict";
import { test } from "node:test";
import { LruCache } from "../gateway.ts";

test("the cache forgets the least recently used key", () => {
  const cache = new LruCache<string>(2);
  cache.set("a", "A");
  cache.set("b", "B");
  cache.get("a");
  cache.set("c", "C");
  assert.deepEqual(cache.keys(), ["a", "c"]);
});
