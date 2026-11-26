import assert from "node:assert/strict";
import { test } from "node:test";
import { map, range, take } from "../seq.ts";

test("the first squares of an endless range", () => {
  assert.deepEqual([...take(map(range(1), (n) => n * n), 4)], [1, 4, 9, 16]);
});
