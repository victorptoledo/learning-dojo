import assert from "node:assert/strict";
import { test } from "node:test";
import { render, screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { ContactsPage } from "../ContactsPage.tsx";
import { renderCount, renderTotal, resetRenders } from "../renderLog.ts";

const contacts = [
  { id: "b", name: "Bob", email: "bob@example.com" },
  { id: "a", name: "Ada", email: "ada@example.com" },
  { id: "c", name: "Cy", email: "cy@example.com" },
];

test("typing a note renders no row and does not sort again", async () => {
  const user = userEvent.setup();
  render(<ContactsPage contacts={contacts} />);
  resetRenders();
  await user.type(screen.getByLabelText("Note"), "call back");
  assert.equal(renderTotal("row:"), 0, "rows rendered while typing a note");
  assert.equal(renderCount("sortContacts"), 0, "sortContacts ran while typing a note");
});
