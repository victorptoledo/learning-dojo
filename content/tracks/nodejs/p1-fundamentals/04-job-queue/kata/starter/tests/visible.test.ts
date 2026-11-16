import assert from "node:assert/strict";
import { test } from "node:test";
import { JobQueue } from "../queue.ts";

test("runs a job and reports it with events", async () => {
  const queue = new JobQueue(async (n: number) => n * 2);
  const done: [number, number][] = [];
  queue.on("done", (id, result) => done.push([id, result]));
  const id = queue.push(21);
  await queue.idle();
  assert.deepEqual(done, [[id, 42]]);
});
