import assert from "node:assert/strict";
import { Readable } from "node:stream";
import { test } from "node:test";
import { splitLines } from "../logs.ts";

test("splits bytes into lines", async () => {
  const lines: string[] = [];
  for await (const line of Readable.from([Buffer.from("first\nsec"), Buffer.from("ond\n")]).pipe(splitLines())) {
    lines.push(line);
  }
  assert.deepEqual(lines, ["first", "second"]);
});
