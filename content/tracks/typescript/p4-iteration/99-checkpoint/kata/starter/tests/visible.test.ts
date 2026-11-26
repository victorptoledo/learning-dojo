import assert from "node:assert/strict";
import { test } from "node:test";
import { report } from "../logs.ts";

test("a report over two chunks", async () => {
  const chunks = ["2026-11-26T10:00:01Z info started\n2026-11-26T10:00:", "07Z error failed\n"];
  const result = await report(chunks, 60_000);
  assert.deepEqual(result.errors, []);
  assert.deepEqual(
    result.windows.map((window) => [window.start.toISOString(), window.counts]),
    [["2026-11-26T10:00:00.000Z", { debug: 0, info: 1, warn: 0, error: 1 }]],
  );
});
