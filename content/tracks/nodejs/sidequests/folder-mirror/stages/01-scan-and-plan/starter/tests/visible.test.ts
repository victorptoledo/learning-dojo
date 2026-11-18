// Your own tests: `npm test` runs every tests/*.test.ts. Add more as you go.
import assert from "node:assert/strict";
import { test } from "node:test";
import { formatPlan } from "../mirror.ts";

test("an empty plan has nothing to do", () => {
  assert.deepEqual(formatPlan([]), ["nothing to do"]);
});
