import assert from "node:assert/strict";
import { test } from "node:test";
import { createResourceCache } from "../resource.ts";

test("read returns the same promise for the same key", () => {
  let loads = 0;
  const cache = createResourceCache(async (key) => {
    loads += 1;
    return key.toUpperCase();
  });
  assert.equal(cache.read("a"), cache.read("a"));
  assert.equal(loads, 1);
});
