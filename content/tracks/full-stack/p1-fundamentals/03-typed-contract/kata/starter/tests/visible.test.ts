import assert from "node:assert/strict";
import { test } from "node:test";
import { NewTodo } from "../contract.ts";
import { handleTodos, TodoStore } from "../server.ts";

test("a valid todo is created with 201", () => {
  const answer = handleTodos(
    { method: "POST", path: "/todos", contentType: "application/json", body: '{"title": "Buy milk"}' },
    new TodoStore(),
  );
  assert.equal(answer.status, 201);
  assert.deepEqual(answer.body, { id: 1, title: "Buy milk", due: null, done: false });
});

test("the contract trims titles", () => {
  assert.deepEqual(NewTodo.parse({ title: "  Buy milk " }), { title: "Buy milk" });
});
