import assert from "node:assert/strict";
import { beforeEach, describe, test } from "node:test";
import { render, screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { ContactsPage } from "../ContactsPage.tsx";
import type { Contact } from "../contacts.ts";
import { renderCount, renderTotal, resetRenders } from "../renderLog.ts";

function makeContacts(): readonly Contact[] {
  return Object.freeze(
    ["Mia", "Ada", "Bob", "Zoe", "Cy", "Ann", "Max", "Eve"].map((name) =>
      Object.freeze({ id: name.toLowerCase(), name, email: `${name.toLowerCase()}@example.com` }),
    ),
  );
}

function names() {
  return screen.getAllByRole("button", { pressed: false }).map((button) => button.textContent);
}

beforeEach(() => resetRenders());

describe("behaviour", () => {
  test("lists the contacts sorted by name, each row rendered once", () => {
    render(<ContactsPage contacts={makeContacts()} />);
    assert.deepEqual(names(), ["Ada", "Ann", "Bob", "Cy", "Eve", "Max", "Mia", "Zoe"]);
    assert.equal(renderTotal("row:"), 8);
    assert.equal(renderCount("row:ada"), 1);
  });

  test("filters by name, ignoring case and spaces", async () => {
    const user = userEvent.setup();
    render(<ContactsPage contacts={makeContacts()} />);
    await user.type(screen.getByLabelText("Filter"), " M ");
    assert.deepEqual(names(), ["Max", "Mia"]);
  });

  test("selects a contact", async () => {
    const user = userEvent.setup();
    render(<ContactsPage contacts={makeContacts()} />);
    assert.ok(screen.getByText("Selected: nobody"));
    await user.click(screen.getByRole("button", { name: "Bob" }));
    assert.equal(screen.getByRole("button", { name: "Bob" }).getAttribute("aria-pressed"), "true");
    assert.ok(screen.getByText("Selected: Bob"));
    await user.click(screen.getByRole("button", { name: "Eve" }));
    assert.equal(screen.getByRole("button", { name: "Bob" }).getAttribute("aria-pressed"), "false");
    assert.ok(screen.getByText("Selected: Eve"));
  });

  test("the note keeps what the user typed", async () => {
    const user = userEvent.setup();
    render(<ContactsPage contacts={makeContacts()} />);
    await user.type(screen.getByLabelText("Note"), "call Bob");
    assert.equal((screen.getByLabelText("Note") as HTMLInputElement).value, "call Bob");
  });
});

describe("budget", () => {
  test("typing a note renders no row and does not sort", async () => {
    const user = userEvent.setup();
    render(<ContactsPage contacts={makeContacts()} />);
    resetRenders();
    await user.type(screen.getByLabelText("Note"), "remember the milk");
    assert.equal(renderTotal("row:"), 0, "row renders while typing a note");
    assert.equal(renderCount("sortContacts"), 0, "sortContacts calls while typing a note");
  });

  test("filtering does not sort again or re-render rows that stay visible", async () => {
    const user = userEvent.setup();
    render(<ContactsPage contacts={makeContacts()} />);
    resetRenders();
    await user.type(screen.getByLabelText("Filter"), "a");
    assert.equal(renderCount("sortContacts"), 0, "sortContacts calls while filtering");
    assert.equal(renderTotal("row:"), 0, "renders of rows that stayed visible");
    await user.clear(screen.getByLabelText("Filter"));
    assert.equal(renderCount("row:ada"), 0, "Ada was visible all along");
    assert.equal(renderCount("row:bob"), 1, "Bob comes back and renders once");
  });

  test("selecting renders only the old and the new selected row", async () => {
    const user = userEvent.setup();
    render(<ContactsPage contacts={makeContacts()} />);
    await user.click(screen.getByRole("button", { name: "Bob" }));
    resetRenders();
    await user.click(screen.getByRole("button", { name: "Zoe" }));
    assert.equal(renderTotal("row:"), 2);
    assert.equal(renderCount("row:bob"), 1);
    assert.equal(renderCount("row:zoe"), 1);
  });

  test("a new contacts array is sorted exactly once", () => {
    const { rerender } = render(<ContactsPage contacts={makeContacts()} />);
    assert.equal(renderCount("sortContacts"), 1);
    resetRenders();
    const more = [...makeContacts(), { id: "kai", name: "Kai", email: "kai@example.com" }];
    rerender(<ContactsPage contacts={more} />);
    assert.equal(renderCount("sortContacts"), 1);
    assert.equal(renderCount("row:kai"), 1);
  });
});
