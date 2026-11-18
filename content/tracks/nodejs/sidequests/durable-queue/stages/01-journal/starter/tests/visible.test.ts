// Your own tests: `npm test` runs every tests/*.test.ts. Add more as you go.
import assert from "node:assert/strict";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { test } from "node:test";
import { openQueue } from "../queue.ts";

test("a job added before a restart is still there after it", async () => {
  const dir = await mkdtemp(path.join(tmpdir(), "queue-"));
  try {
    const queue = await openQueue(dir);
    await queue.add("email", { to: "ana@example.com" });
    await queue.close();
    const again = await openQueue(dir);
    assert.deepEqual(again.list().map((job) => job.type), ["email"]);
    await again.close();
  } finally {
    await rm(dir, { recursive: true, force: true });
  }
});
