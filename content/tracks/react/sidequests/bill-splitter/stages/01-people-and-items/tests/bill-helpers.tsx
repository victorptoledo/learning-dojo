// Helpers shared by the contract tests of every stage: they act like a user and read the page
// only through roles, labels and visible text.
import { render, screen, within } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { BillSplitter } from "../BillSplitter.tsx";

export type User = ReturnType<typeof userEvent.setup>;

export function start(): User {
  const user = userEvent.setup();
  render(<BillSplitter />);
  return user;
}

export const nameBox = () => screen.getByRole("textbox", { name: "Name" });
export const itemBox = () => screen.getByRole("textbox", { name: "Item" });
export const priceBox = () => screen.getByRole("textbox", { name: "Price" });

export async function addPerson(user: User, name: string): Promise<void> {
  await user.clear(nameBox());
  if (name !== "") {
    await user.type(nameBox(), name);
  }
  await user.click(screen.getByRole("button", { name: "Add person" }));
}

export async function addItem(user: User, description: string, price: string): Promise<void> {
  await user.clear(itemBox());
  await user.clear(priceBox());
  if (description !== "") {
    await user.type(itemBox(), description);
  }
  if (price !== "") {
    await user.type(priceBox(), price);
  }
  await user.click(screen.getByRole("button", { name: "Add item" }));
}

/** The text of the elements an input points at with aria-describedby ("" when none). */
export function describedBy(input: HTMLElement): string {
  const ids = (input.getAttribute("aria-describedby") ?? "").split(/\s+/).filter(Boolean);
  return ids
    .map((id) => document.getElementById(id)?.textContent?.trim() ?? "")
    .filter(Boolean)
    .join(" ");
}

/** The names in a list, read from its "Remove <name>" buttons, in order. */
export function namesIn(list: "People" | "Items"): string[] {
  const element = screen.queryByRole("list", { name: list });
  if (!element) {
    return [];
  }
  return within(element)
    .queryAllByRole("button", { name: /^Remove / })
    .map((button) => (button.textContent ?? "").replace(/^Remove\s+/, "").trim());
}

/** The list item of an item, found by its Remove button. */
export function itemRow(description: string): HTMLElement {
  const button = within(screen.getByRole("list", { name: "Items" })).getByRole("button", {
    name: `Remove ${description}`,
  });
  const row = button.closest("li");
  if (!row) {
    throw new Error(`the Remove ${description} button is not inside a list item`);
  }
  return row;
}

/** The text of every list item of the Shares list, in order. */
export function shares(): string[] {
  const list = screen.queryByRole("list", { name: "Shares" });
  if (!list) {
    return [];
  }
  return within(list)
    .getAllByRole("listitem")
    .map((item) => (item.textContent ?? "").replace(/\s+/g, " ").trim());
}
