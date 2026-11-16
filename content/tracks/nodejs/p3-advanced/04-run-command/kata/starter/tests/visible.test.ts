import assert from "node:assert/strict";
import { test } from "node:test";
import { run } from "../run.ts";

test("runs a child process and collects its output and exit code", async () => {
  const result = await run(process.execPath, ["-e", "console.log('hi'); process.exitCode = 1"]);
  assert.equal(result.stdout, "hi\n");
  assert.equal(result.code, 1);
});
