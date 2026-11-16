import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { render, screen, within } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { TodoBoard } from "../TodoBoard.tsx";
import { addTodo, clearDone, remaining, toggleTodo, visibleTodos, type Todo } from "../todos.ts";

const todos: readonly Todo[] = Object.freeze([
  Object.freeze({ id: "1", title: "Read", done: false }),
  Object.freeze({ id: "2", title: "Write", done: true }),
  Object.freeze({ id: "3", title: "Ship", done: false }),
]);

function ids() {
  let next = 10;
  return () => String(next++);
}

function items() {
  return within(screen.getByRole("list", { name: "Tasks" }))
    .queryAllByRole("checkbox")
    .map((box) => box.closest("li")?.textContent);
}

describe("update functions", () => {
  test("addTodo keeps the same list for a blank title", () => {
    assert.equal(addTodo(todos, "   ", "x"), todos);
  });

  test("toggleTodo copies only the toggled todo", () => {
    const next = toggleTodo(todos, "1");
    assert.notEqual(next, todos);
    assert.deepEqual(next[0], { id: "1", title: "Read", done: true });
    assert.equal(next[1], todos[1]);
    assert.equal(todos[0]?.done, false);
  });

  test("clearDone, visibleTodos and remaining", () => {
    assert.deepEqual(
      clearDone(todos).map((todo) => todo.id),
      ["1", "3"],
    );
    assert.deepEqual(
      visibleTodos(todos, "active").map((todo) => todo.id),
      ["1", "3"],
    );
    assert.deepEqual(
      visibleTodos(todos, "done").map((todo) => todo.id),
      ["2"],
    );
    assert.equal(visibleTodos(todos, "all").length, 3);
    assert.equal(remaining(todos), 2);
  });
});

describe("TodoBoard", () => {
  test("shows the initial todos with their checkboxes", () => {
    render(<TodoBoard initialTodos={todos} />);
    assert.deepEqual(items(), ["Read", "Write", "Ship"]);
    const write = screen.getByRole("checkbox", { name: "Write" }) as HTMLInputElement;
    assert.equal(write.checked, true);
    assert.ok(screen.getByText("2 items left"));
  });

  test("adds a task from the form and empties the input", async () => {
    const user = userEvent.setup();
    render(<TodoBoard createId={ids()} />);
    const input = screen.getByLabelText("New task") as HTMLInputElement;
    await user.type(input, "  Learn hooks ");
    await user.click(screen.getByRole("button", { name: "Add" }));
    assert.deepEqual(items(), ["Learn hooks"]);
    assert.equal(input.value, "");
    await user.type(input, "Test it{Enter}");
    assert.deepEqual(items(), ["Learn hooks", "Test it"]);
    assert.ok(screen.getByText("2 items left"));
  });

  test("a blank title adds nothing", async () => {
    const user = userEvent.setup();
    render(<TodoBoard createId={ids()} />);
    await user.type(screen.getByLabelText("New task"), "   {Enter}");
    assert.ok(screen.getByText("Nothing to show"));
    assert.ok(screen.getByText("0 items left"));
  });

  test("toggling a checkbox updates the counter", async () => {
    const user = userEvent.setup();
    render(<TodoBoard initialTodos={todos} />);
    await user.click(screen.getByRole("checkbox", { name: "Read" }));
    assert.ok(screen.getByText("1 item left"));
    const read = screen.getByRole("checkbox", { name: "Read" }) as HTMLInputElement;
    assert.equal(read.checked, true);
  });

  test("the filters show the matching todos and mark the pressed button", async () => {
    const user = userEvent.setup();
    render(<TodoBoard initialTodos={todos} />);
    const all = screen.getByRole("button", { name: "All" });
    const active = screen.getByRole("button", { name: "Active" });
    const done = screen.getByRole("button", { name: "Done" });
    assert.equal(all.getAttribute("aria-pressed"), "true");
    assert.equal(active.getAttribute("aria-pressed"), "false");
    await user.click(active);
    assert.deepEqual(items(), ["Read", "Ship"]);
    assert.equal(active.getAttribute("aria-pressed"), "true");
    assert.equal(all.getAttribute("aria-pressed"), "false");
    await user.click(done);
    assert.deepEqual(items(), ["Write"]);
    assert.ok(screen.getByText("2 items left"));
  });

  test("an empty filter says so", async () => {
    const user = userEvent.setup();
    render(<TodoBoard initialTodos={[{ id: "1", title: "Read", done: false }]} />);
    await user.click(screen.getByRole("button", { name: "Done" }));
    assert.ok(screen.getByText("Nothing to show"));
    assert.equal(screen.queryByRole("list", { name: "Tasks" }), null);
  });

  test("Clear done removes the done todos and is disabled when there are none", async () => {
    const user = userEvent.setup();
    render(<TodoBoard initialTodos={todos} />);
    const clear = screen.getByRole("button", { name: "Clear done" }) as HTMLButtonElement;
    assert.equal(clear.disabled, false);
    await user.click(clear);
    assert.deepEqual(items(), ["Read", "Ship"]);
    assert.equal(clear.disabled, true);
  });

  test("checkbox state follows the todo, not its position", async () => {
    const user = userEvent.setup();
    render(<TodoBoard initialTodos={todos} />);
    await user.click(screen.getByRole("button", { name: "Clear done" }));
    await user.click(screen.getByRole("checkbox", { name: "Ship" }));
    const read = screen.getByRole("checkbox", { name: "Read" }) as HTMLInputElement;
    const ship = screen.getByRole("checkbox", { name: "Ship" }) as HTMLInputElement;
    assert.equal(read.checked, false);
    assert.equal(ship.checked, true);
  });
});
