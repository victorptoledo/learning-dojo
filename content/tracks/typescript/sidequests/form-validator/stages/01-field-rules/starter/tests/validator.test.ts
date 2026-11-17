// Your own tests: `npm test` runs every tests/*.test.ts. Add more as you go.
import assert from "node:assert/strict";
import { test } from "node:test";
import { checkField } from "../validator.ts";

test("an empty required field", () => {
  assert.deepEqual(checkField([{ kind: "required" }], ""), ["is required"]);
});
