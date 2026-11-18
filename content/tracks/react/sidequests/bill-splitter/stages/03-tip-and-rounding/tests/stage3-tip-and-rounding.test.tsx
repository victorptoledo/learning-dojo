import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { screen, within } from "@testing-library/react";
import { addItem, addPerson, describedBy, itemRow, shares, start, type User } from "./bill-helpers.tsx";
import { expectAbsent } from "./bill-helpers.tsx";

const tipGroup = () => screen.getByRole("group", { name: "Tip" });
const radio = (name: string) => within(tipGroup()).getByRole("radio", { name }) as HTMLInputElement;
const customBox = () => screen.queryByRole("textbox", { name: "Custom tip (%)" });

function totals(): { tip: string; total: string } {
  return {
    tip: screen.getByText(/^Tip: /).textContent ?? "",
    total: screen.getByText(/^Total: /).textContent ?? "",
  };
}

async function table(user: User, ...names: string[]): Promise<void> {
  for (const name of names) {
    await addPerson(user, name);
  }
}

const cents = (text: string) => Math.round(Number(text.split(/: | owes /)[1]) * 100);

describe("tip", () => {
  test("there is no tip until one is chosen", async () => {
    const user = start();
    assert.deepEqual(
      within(tipGroup())
        .getAllByRole("radio")
        .map((box) => (box as HTMLInputElement).labels?.[0]?.textContent?.trim()),
      ["No tip", "10%", "15%", "Custom"],
    );
    assert.equal(radio("No tip").checked, true);
    await addItem(user, "Pizza", "32.50");
    assert.deepEqual(totals(), { tip: "Tip: 0.00", total: "Total: 32.50" });
  });

  test("10% and 15% of the subtotal", async () => {
    const user = start();
    await addItem(user, "Pizza", "32.50");
    await user.click(radio("10%"));
    assert.equal(radio("10%").checked, true);
    assert.equal(radio("No tip").checked, false);
    assert.deepEqual(totals(), { tip: "Tip: 3.25", total: "Total: 35.75" });
    await user.click(radio("15%"));
    assert.deepEqual(totals(), { tip: "Tip: 4.88", total: "Total: 37.38" });
  });

  test("half a cent of tip rounds up", async () => {
    const user = start();
    await addItem(user, "Gum", "0.10");
    await user.click(radio("15%"));
    assert.deepEqual(totals(), { tip: "Tip: 0.02", total: "Total: 0.12" });
  });

  test("the subtotal stays the price of the items", async () => {
    const user = start();
    await addItem(user, "Pizza", "20");
    await user.click(radio("10%"));
    assert.ok(screen.getByText("Subtotal: 20.00"));
  });

  test("a custom tip is a whole percentage typed in its own field", async () => {
    const user = start();
    await addItem(user, "Pizza", "50");
    expectAbsent(customBox());
    await user.click(radio("Custom"));
    const box = customBox();
    assert.ok(box);
    assert.deepEqual(totals(), { tip: "Tip: 0.00", total: "Total: 50.00" });
    await user.type(box, "12");
    assert.deepEqual(totals(), { tip: "Tip: 6.00", total: "Total: 56.00" });
    assert.equal(describedBy(box), "");
  });

  test("a custom tip that is not 0 to 100 is an error and counts as no tip", async () => {
    const user = start();
    await addItem(user, "Pizza", "50");
    await user.click(radio("Custom"));
    for (const text of ["101", "abc", "2.5", "-1"]) {
      const box = customBox();
      assert.ok(box);
      await user.clear(box);
      await user.type(box, text);
      assert.equal(box.getAttribute("aria-invalid"), "true", text);
      assert.equal(describedBy(box), "Enter a tip from 0 to 100", text);
      assert.deepEqual(totals(), { tip: "Tip: 0.00", total: "Total: 50.00" }, text);
    }
    const box = customBox();
    assert.ok(box);
    await user.clear(box);
    await user.type(box, "100");
    assert.notEqual(box.getAttribute("aria-invalid"), "true");
    assert.deepEqual(totals(), { tip: "Tip: 50.00", total: "Total: 100.00" });
  });

  test("choosing another tip hides the custom field", async () => {
    const user = start();
    await addItem(user, "Pizza", "10");
    await user.click(radio("Custom"));
    await user.click(radio("10%"));
    expectAbsent(customBox());
    assert.deepEqual(totals(), { tip: "Tip: 1.00", total: "Total: 11.00" });
  });
});

describe("what each person owes", () => {
  test("the tip is split in proportion to what each person had", async () => {
    const user = start();
    await table(user, "Ana", "Bia");
    await addItem(user, "Salad", "10");
    await addItem(user, "Steak", "30");
    await user.click(within(within(itemRow("Salad")).getByRole("group")).getByRole("checkbox", { name: "Bia" }));
    await user.click(within(within(itemRow("Steak")).getByRole("group")).getByRole("checkbox", { name: "Ana" }));
    await user.click(radio("10%"));
    assert.deepEqual(shares(), ["Ana owes 11.00", "Bia owes 33.00"]);
  });

  test("leftover tip cents go to the largest remainders, ties in table order", async () => {
    const user = start();
    await table(user, "Ana", "Bia", "Cai");
    await addItem(user, "Cake", "10");
    await user.click(radio("15%"));
    assert.deepEqual(shares(), ["Ana owes 3.84", "Bia owes 3.83", "Cai owes 3.83"]);
  });

  test("everyone's share plus the unassigned part is the total", async () => {
    const user = start();
    await table(user, "Ana", "Bia", "Cai");
    await addItem(user, "A", "10.01");
    await addItem(user, "B", "7.77");
    await addItem(user, "C", "3.33");
    await user.click(within(within(itemRow("A")).getByRole("group")).getByRole("checkbox", { name: "Cai" }));
    await user.click(within(within(itemRow("C")).getByRole("group")).getByRole("checkbox", { name: "Ana" }));
    for (const tip of ["10%", "15%"]) {
      await user.click(radio(tip));
      const total = cents(totals().total);
      const owed = shares().reduce((sum, line) => sum + cents(line), 0);
      assert.equal(owed, total, tip);
      expectAbsent(screen.queryByText(/^Unassigned/), tip);
    }
    await user.click(radio("Custom"));
    const box = customBox();
    assert.ok(box);
    await user.type(box, "7");
    assert.equal(totals().total, "Total: 22.59");
    assert.deepEqual(shares(), ["Ana owes 8.13", "Bia owes 9.91", "Cai owes 4.55"]);
  });

  test("with nobody to pay it, the tip is unassigned too", async () => {
    const user = start();
    await addItem(user, "Bread", "5");
    await user.click(radio("10%"));
    assert.ok(screen.getByText("Unassigned: 5.50"));
  });

  test("the people with a share pay the whole tip", async () => {
    const user = start();
    await table(user, "Ana");
    await addItem(user, "Pizza", "20");
    await addItem(user, "Juice", "10");
    await user.click(within(within(itemRow("Juice")).getByRole("group")).getByRole("checkbox", { name: "Ana" }));
    await user.click(radio("10%"));
    assert.deepEqual(totals(), { tip: "Tip: 3.00", total: "Total: 33.00" });
    assert.ok(screen.getByText("Unassigned: 10.00"));
    assert.deepEqual(shares(), ["Ana owes 23.00"]);
  });
});
