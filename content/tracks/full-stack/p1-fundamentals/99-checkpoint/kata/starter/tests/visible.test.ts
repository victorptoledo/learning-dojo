import assert from "node:assert/strict";
import { test } from "node:test";
import { openNotes } from "../db.ts";
import { validateNote } from "../notes.ts";

test("a blank title is refused", () => {
  assert.deepEqual(validateNote({ title: "   " }), { ok: false, errors: { title: "Title is required" } });
});

test("the repository stores and lists notes, newest first", () => {
  const repo = openNotes();
  repo.create({ title: "first", body: "" });
  repo.create({ title: "second", body: "hello" });
  assert.deepEqual(repo.list(), [
    { id: 2, title: "second", body: "hello" },
    { id: 1, title: "first", body: "" },
  ]);
});
