// Your own tests: `npm test` compiles the project and runs every tests/*.test.tsx. Add more as you go.
import assert from "node:assert/strict";
import { test } from "node:test";
import { render, screen } from "@testing-library/react";
import { Board, BoardProvider } from "../Board.tsx";

test("a column is a region named by its title", () => {
  const initial = { columns: [{ id: "todo", title: "To do", cards: [{ id: "1", title: "Read" }] }] };
  render(
    <BoardProvider initial={initial}>
      <Board />
    </BoardProvider>,
  );
  assert.ok(screen.getByRole("region", { name: "To do" }));
  assert.ok(screen.getByRole("button", { name: "Edit Read" }));
});
