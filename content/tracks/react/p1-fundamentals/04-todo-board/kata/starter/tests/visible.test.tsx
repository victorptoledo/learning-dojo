import assert from "node:assert/strict";
import { test } from "node:test";
import { addTodo } from "../todos.ts";

test("addTodo appends a trimmed, not done todo", () => {
  const todos = Object.freeze([{ id: "a", title: "Read", done: false }]);
  assert.deepEqual(addTodo(todos, "  Write  ", "b"), [
    { id: "a", title: "Read", done: false },
    { id: "b", title: "Write", done: false },
  ]);
});
