import assert from "node:assert/strict";
import { test } from "node:test";
import { formatReport } from "../dupes.ts";

test("the report lists a group and the wasted bytes", () => {
  assert.deepEqual(formatReport([{ size: 5, hash: "abc", files: ["a.txt", "b.txt"] }]), [
    "2 files, 5 bytes each:",
    "  a.txt",
    "  b.txt",
    "1 duplicate files waste 5 bytes",
  ]);
});
