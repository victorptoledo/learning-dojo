import assert from "node:assert/strict";
import { test } from "node:test";
import { parseLine } from "../ledger.ts";

test("parses a valid line", () => {
  assert.deepEqual(parseLine("t1, 2026-03-14, expense, 12.50, food"), {
    ok: true,
    value: { id: "t1", date: "2026-03-14", kind: "expense", amountCents: 1250, category: "food" },
  });
});
