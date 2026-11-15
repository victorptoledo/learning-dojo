import assert from "node:assert/strict";
import { test } from "node:test";
import { retry } from "../retry.ts";

test("retry returns the first success", async () => {
  let calls = 0;
  const value = await retry(
    async (attempt) => {
      calls += 1;
      if (attempt < 3) {
        throw new Error(`attempt ${attempt} failed`);
      }
      return "ok";
    },
    { attempts: 5, delayMs: 1 },
  );
  assert.equal(value, "ok");
  assert.equal(calls, 3);
});
