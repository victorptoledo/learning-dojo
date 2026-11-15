import assert from "node:assert/strict";
import { test } from "node:test";
import { greet } from "../greet.ts";

test("greets by name", () => {
  assert.equal(greet("Ada"), "Welcome to the dojo, Ada!");
});
