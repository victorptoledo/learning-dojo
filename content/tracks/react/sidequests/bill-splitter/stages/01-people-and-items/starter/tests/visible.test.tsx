// Your own tests: `npm test` compiles the project and runs every tests/*.test.tsx. Add more as you go.
import assert from "node:assert/strict";
import { test } from "node:test";
import { render, screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { BillSplitter } from "../BillSplitter.tsx";

test("a person added by name shows up in the People list", async () => {
  const user = userEvent.setup();
  render(<BillSplitter />);
  await user.type(screen.getByRole("textbox", { name: "Name" }), "Ana");
  await user.click(screen.getByRole("button", { name: "Add person" }));
  assert.ok(screen.getByRole("button", { name: "Remove Ana" }));
});
