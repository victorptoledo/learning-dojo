import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { screen, within } from "@testing-library/react";
import { addItem, addPerson, itemRow, shares, start, type User } from "./bill-helpers.tsx";
import { expectAbsent } from "./bill-helpers.tsx";

function boxes(item: string): HTMLInputElement[] {
  const group = within(itemRow(item)).getByRole("group", { name: `Who had ${item}?` });
  return within(group).getAllByRole("checkbox") as HTMLInputElement[];
}

function checked(item: string): Record<string, boolean> {
  const group = within(itemRow(item)).getByRole("group", { name: `Who had ${item}?` });
  const result: Record<string, boolean> = {};
  for (const person of ["Ana", "Bia", "Cai", "Dan"]) {
    const box = within(group).queryByRole("checkbox", { name: person }) as HTMLInputElement | null;
    if (box) {
      result[person] = box.checked;
    }
  }
  return result;
}

async function toggle(user: User, item: string, person: string): Promise<void> {
  const group = within(itemRow(item)).getByRole("group", { name: `Who had ${item}?` });
  await user.click(within(group).getByRole("checkbox", { name: person }));
}

async function table(user: User, ...names: string[]): Promise<void> {
  for (const name of names) {
    await addPerson(user, name);
  }
}

describe("who had what", () => {
  test("a new item is shared by everyone at the table, in their order", async () => {
    const user = start();
    await table(user, "Ana", "Bia");
    await addItem(user, "Pizza", "12.50");
    assert.deepEqual(
      boxes("Pizza").map((box) => box.labels?.[0]?.textContent?.trim()),
      ["Ana", "Bia"],
    );
    assert.deepEqual(checked("Pizza"), { Ana: true, Bia: true });
  });

  test("each person's share is listed and the item is split evenly", async () => {
    const user = start();
    await table(user, "Ana", "Bia");
    await addItem(user, "Pizza", "12.50");
    assert.deepEqual(shares(), ["Ana owes 6.25", "Bia owes 6.25"]);
  });

  test("unchecking a person moves their part to the others", async () => {
    const user = start();
    await table(user, "Ana", "Bia");
    await addItem(user, "Pizza", "12.50");
    await addItem(user, "Juice", "4");
    await toggle(user, "Pizza", "Bia");
    assert.deepEqual(checked("Pizza"), { Ana: true, Bia: false });
    assert.deepEqual(shares(), ["Ana owes 14.50", "Bia owes 2.00"]);
    await toggle(user, "Pizza", "Bia");
    assert.deepEqual(shares(), ["Ana owes 8.25", "Bia owes 8.25"]);
  });

  test("leftover cents go one each to the first people of the table", async () => {
    const user = start();
    await table(user, "Ana", "Bia", "Cai");
    await addItem(user, "Cake", "10");
    assert.deepEqual(shares(), ["Ana owes 3.34", "Bia owes 3.33", "Cai owes 3.33"]);
    await addItem(user, "Candy", "0.05");
    assert.deepEqual(shares(), ["Ana owes 3.36", "Bia owes 3.35", "Cai owes 3.34"]);
  });

  test("the first people means table order, not the order they were checked", async () => {
    const user = start();
    await table(user, "Ana", "Bia", "Cai");
    await addItem(user, "Gum", "0.01");
    await toggle(user, "Gum", "Ana");
    assert.deepEqual(shares(), ["Ana owes 0.00", "Bia owes 0.01", "Cai owes 0.00"]);
    await toggle(user, "Gum", "Ana");
    assert.deepEqual(shares(), ["Ana owes 0.01", "Bia owes 0.00", "Cai owes 0.00"]);
  });

  test("someone who arrives later did not have the items already ordered", async () => {
    const user = start();
    await table(user, "Ana");
    await addItem(user, "Pizza", "10");
    await addPerson(user, "Bia");
    assert.deepEqual(checked("Pizza"), { Ana: true, Bia: false });
    assert.deepEqual(shares(), ["Ana owes 10.00", "Bia owes 0.00"]);
    await addItem(user, "Juice", "3");
    assert.deepEqual(checked("Juice"), { Ana: true, Bia: true });
  });

  test("an item nobody had is unassigned and says so", async () => {
    const user = start();
    await table(user, "Ana");
    await addItem(user, "Pizza", "12.50");
    expectAbsent(screen.queryByText(/^Unassigned/));
    await toggle(user, "Pizza", "Ana");
    assert.ok(within(itemRow("Pizza")).getByText("Nobody had Pizza"));
    assert.ok(screen.getByText("Unassigned: 12.50"));
    assert.deepEqual(shares(), ["Ana owes 0.00"]);
    await toggle(user, "Pizza", "Ana");
    expectAbsent(screen.queryByText("Nobody had Pizza"));
    expectAbsent(screen.queryByText(/^Unassigned/));
  });

  test("items added before anyone sits down are unassigned", async () => {
    const user = start();
    await addItem(user, "Bread", "5");
    assert.ok(screen.getByText("Unassigned: 5.00"));
    expectAbsent(screen.queryByRole("list", { name: "Shares" }));
  });

  test("removing a person removes them from every item", async () => {
    const user = start();
    await table(user, "Ana", "Bia");
    await addItem(user, "Pizza", "10");
    await toggle(user, "Pizza", "Ana");
    await addItem(user, "Juice", "4");
    await user.click(screen.getByRole("button", { name: "Remove Bia" }));
    assert.deepEqual(checked("Pizza"), { Ana: false });
    assert.deepEqual(checked("Juice"), { Ana: true });
    assert.deepEqual(shares(), ["Ana owes 4.00"]);
    assert.ok(screen.getByText("Unassigned: 10.00"));
  });

  test("a person removed and added again is a new person", async () => {
    const user = start();
    await table(user, "Ana", "Bia");
    await addItem(user, "Pizza", "10");
    await user.click(screen.getByRole("button", { name: "Remove Bia" }));
    await addPerson(user, "Bia");
    assert.deepEqual(checked("Pizza"), { Ana: true, Bia: false });
    assert.deepEqual(shares(), ["Ana owes 10.00", "Bia owes 0.00"]);
  });

  test("removing an item removes it from the shares", async () => {
    const user = start();
    await table(user, "Ana", "Bia");
    await addItem(user, "Pizza", "10");
    await addItem(user, "Juice", "3");
    await user.click(screen.getByRole("button", { name: "Remove Pizza" }));
    assert.deepEqual(shares(), ["Ana owes 1.50", "Bia owes 1.50"]);
  });

  test("the shares and the unassigned part always add up to the subtotal", async () => {
    const user = start();
    await table(user, "Ana", "Bia", "Cai");
    await addItem(user, "A", "10.01");
    await addItem(user, "B", "7.77");
    await addItem(user, "C", "0.02");
    await toggle(user, "A", "Cai");
    await toggle(user, "B", "Ana");
    await toggle(user, "C", "Ana");
    await toggle(user, "C", "Bia");
    await toggle(user, "C", "Cai");
    const owed = shares().map((line) => Math.round(Number(line.split(" owes ")[1]) * 100));
    const unassigned = Math.round(Number(screen.getByText(/^Unassigned: /).textContent?.split(": ")[1]) * 100);
    assert.equal(owed.reduce((sum, cents) => sum + cents, 0) + unassigned, 1780);
    assert.deepEqual(shares(), ["Ana owes 5.01", "Bia owes 8.89", "Cai owes 3.88"]);
  });
});
