import assert from "node:assert/strict";
import { test } from "node:test";
import { area } from "../shapes.ts";

test("area of a rectangle", () => {
  assert.equal(area({ kind: "rectangle", width: 3, height: 4 }), 12);
});
