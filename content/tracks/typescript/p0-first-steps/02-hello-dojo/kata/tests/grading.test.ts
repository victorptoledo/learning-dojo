import assert from "node:assert/strict";
import { test } from "node:test";
import { greet, main, parseArgs } from "../greet.ts";

test("trims the name", () => {
  assert.equal(greet("  Grace  "), "Welcome to the dojo, Grace!");
});

test("falls back to stranger when the name is missing or blank", () => {
  assert.equal(greet(), "Welcome to the dojo, stranger!");
  assert.equal(greet(""), "Welcome to the dojo, stranger!");
  assert.equal(greet("   "), "Welcome to the dojo, stranger!");
});

test("shouts in upper case", () => {
  assert.equal(greet("Ada", { shout: true }), "WELCOME TO THE DOJO, ADA!");
  assert.equal(greet("Ada", { shout: false }), "Welcome to the dojo, Ada!");
});

test("parses the name from the words", () => {
  assert.deepEqual(parseArgs(["Ada", "Lovelace"]), { name: "Ada Lovelace", shout: false });
});

test("parses --shout anywhere", () => {
  assert.deepEqual(parseArgs(["--shout", "Ada"]), { name: "Ada", shout: true });
  assert.deepEqual(parseArgs(["Ada", "--shout"]), { name: "Ada", shout: true });
  assert.deepEqual(parseArgs([]), { name: "", shout: false });
});

test("main prints the greeting and returns 0", () => {
  const lines: string[] = [];
  const code = main(["Grace", "--shout"], (line) => lines.push(line));
  assert.equal(code, 0);
  assert.deepEqual(lines, ["WELCOME TO THE DOJO, GRACE!"]);
});

test("main greets a stranger without arguments", () => {
  const lines: string[] = [];
  main([], (line) => lines.push(line));
  assert.deepEqual(lines, ["Welcome to the dojo, stranger!"]);
});
