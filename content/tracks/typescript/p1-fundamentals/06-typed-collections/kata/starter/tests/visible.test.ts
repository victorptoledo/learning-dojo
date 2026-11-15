import assert from "node:assert/strict";
import { test } from "node:test";
import { pluck } from "../collections.ts";

test("pluck takes one property of each item", () => {
  const users = [
    { id: 1, name: "Ada" },
    { id: 2, name: "Grace" },
  ];
  assert.deepEqual(pluck(users, "name"), ["Ada", "Grace"]);
});
