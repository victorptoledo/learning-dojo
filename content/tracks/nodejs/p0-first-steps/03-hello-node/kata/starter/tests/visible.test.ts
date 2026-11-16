import assert from "node:assert/strict";
import { test } from "node:test";
import { main, type Io } from "../cli.ts";

test("greets every name", () => {
  const out: string[] = [];
  const io: Io = { env: {}, stdout: (line) => out.push(line), stderr: () => {} };
  assert.equal(main(["Ada", "Linus"], io), 0);
  assert.deepEqual(out, ["Hello, Ada!", "Hello, Linus!"]);
});
