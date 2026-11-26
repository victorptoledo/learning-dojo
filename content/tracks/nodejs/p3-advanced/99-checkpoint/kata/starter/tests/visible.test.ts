import assert from "node:assert/strict";
import { test } from "node:test";
import { parseManifest, runJobs } from "../runner.ts";

test("runs an echo job from a manifest", async () => {
  const manifest = parseManifest('{"jobs": [{"id": "hi", "kind": "tool", "tool": "echo", "args": ["hello"]}]}');
  assert.deepEqual(await runJobs(manifest, { root: "." }), [{ id: "hi", ok: true, output: "hello" }]);
});
