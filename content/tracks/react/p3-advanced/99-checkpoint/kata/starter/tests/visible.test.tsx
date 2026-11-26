import assert from "node:assert/strict";
import { test } from "node:test";
import { toggleIssue, type IssueState } from "../issues.ts";

test("toggling an issue flips done and keeps the other issues as they were", () => {
  const state: IssueState = {
    issues: [
      { id: "a", title: "Login fails", done: false },
      { id: "b", title: "Typo on home", done: true },
    ],
  };
  const next = toggleIssue(state, "a");
  assert.deepEqual(next.issues[0], { id: "a", title: "Login fails", done: true });
  assert.equal(next.issues[1], state.issues[1], "an issue that did not change is the same object");
  assert.equal(state.issues[0]?.done, false, "the old state is not mutated");
});
