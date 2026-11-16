import assert from "node:assert/strict";
import { test } from "node:test";
import { etagOf, matches } from "../server.ts";

test("an ETag is a quoted hash of the body", () => {
  assert.match(etagOf('{"id":1}'), /^"[A-Za-z0-9_-]{16}"$/);
  assert.equal(etagOf('{"id":1}'), etagOf('{"id":1}'));
});

test("an exact ETag matches", () => {
  assert.equal(matches('"abc"', '"abc"'), true);
});
