import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { screen } from "@testing-library/react";
import {
  addItem,
  addPerson,
  describedBy,
  itemBox,
  itemRow,
  nameBox,
  namesIn,
  priceBox,
  start,
} from "./bill-helpers.tsx";

const subtotal = (text: string) => assert.ok(screen.getByText(`Subtotal: ${text}`));

describe("people", () => {
  test("the bill starts empty", () => {
    start();
    assert.ok(screen.getByRole("form", { name: "Add a person" }));
    assert.ok(screen.getByText("No people yet"));
    assert.equal(screen.queryByRole("list", { name: "People" }), null);
    subtotal("0.00");
  });

  test("added people are listed in order with their names trimmed", async () => {
    const user = start();
    await addPerson(user, "  Ana ");
    await addPerson(user, "Bia");
    assert.deepEqual(namesIn("People"), ["Ana", "Bia"]);
    assert.equal(screen.queryByText("No people yet"), null);
  });

  test("adding clears the name and keeps the focus on it", async () => {
    const user = start();
    await addPerson(user, "Ana");
    assert.equal((nameBox() as HTMLInputElement).value, "");
    assert.equal(document.activeElement, nameBox());
  });

  test("Enter in the name field adds the person", async () => {
    const user = start();
    await user.type(nameBox(), "Cai{Enter}");
    assert.deepEqual(namesIn("People"), ["Cai"]);
  });

  test("an empty name is an error tied to the field", async () => {
    const user = start();
    await addPerson(user, "   ");
    assert.equal(nameBox().getAttribute("aria-invalid"), "true");
    assert.equal(describedBy(nameBox()), "Enter a name");
    assert.ok(screen.getByText("Enter a name"));
    assert.equal(document.activeElement, nameBox());
    assert.deepEqual(namesIn("People"), []);
  });

  test("a name already on the list is refused, whatever the case", async () => {
    const user = start();
    await addPerson(user, "Ana");
    await addPerson(user, " ana ");
    assert.equal(describedBy(nameBox()), "ana is already on the list");
    assert.deepEqual(namesIn("People"), ["Ana"]);
  });

  test("the error goes away once a valid name is added", async () => {
    const user = start();
    await addPerson(user, "");
    await addPerson(user, "Ana");
    assert.notEqual(nameBox().getAttribute("aria-invalid"), "true");
    assert.equal(describedBy(nameBox()), "");
    assert.equal(screen.queryByText("Enter a name"), null);
  });

  test("a person can be removed", async () => {
    const user = start();
    await addPerson(user, "Ana");
    await addPerson(user, "Bia");
    await user.click(screen.getByRole("button", { name: "Remove Ana" }));
    assert.deepEqual(namesIn("People"), ["Bia"]);
    await user.click(screen.getByRole("button", { name: "Remove Bia" }));
    assert.ok(screen.getByText("No people yet"));
  });
});

describe("items", () => {
  test("the items list starts empty", () => {
    start();
    assert.ok(screen.getByRole("form", { name: "Add an item" }));
    assert.ok(screen.getByText("No items yet"));
    assert.equal(screen.queryByRole("list", { name: "Items" }), null);
  });

  test("added items show their price with two decimals and add up", async () => {
    const user = start();
    await addItem(user, " Pizza ", "12,5");
    await addItem(user, "Juice", "20");
    assert.deepEqual(namesIn("Items"), ["Pizza", "Juice"]);
    assert.ok(screen.getByText("12.50"));
    assert.ok(screen.getByText("20.00"));
    subtotal("32.50");
    assert.equal(screen.queryByText("No items yet"), null);
  });

  test("each row shows its own description and price", async () => {
    const user = start();
    await addItem(user, "Water", "3.05");
    const row = itemRow("Water");
    assert.match(row.textContent ?? "", /Water/);
    assert.match(row.textContent ?? "", /3\.05/);
  });

  test("adding clears both fields and puts the focus back on Item", async () => {
    const user = start();
    await addItem(user, "Pizza", "10");
    assert.equal((itemBox() as HTMLInputElement).value, "");
    assert.equal((priceBox() as HTMLInputElement).value, "");
    assert.equal(document.activeElement, itemBox());
  });

  test("both empty fields get their error, and the focus goes to the first one", async () => {
    const user = start();
    await addItem(user, "", "");
    assert.equal(describedBy(itemBox()), "Enter an item");
    assert.equal(describedBy(priceBox()), "Enter a price");
    assert.equal(itemBox().getAttribute("aria-invalid"), "true");
    assert.equal(priceBox().getAttribute("aria-invalid"), "true");
    assert.equal(document.activeElement, itemBox());
    assert.deepEqual(namesIn("Items"), []);
  });

  test("a price that is not money is refused and gets the focus", async () => {
    const user = start();
    for (const price of ["abc", "12.345", "1e3", "-4", "1.2.3"]) {
      await addItem(user, "Pizza", price);
      assert.equal(describedBy(priceBox()), "Enter a price like 12.50", price);
      assert.notEqual(itemBox().getAttribute("aria-invalid"), "true", price);
      assert.equal(document.activeElement, priceBox(), price);
    }
    assert.deepEqual(namesIn("Items"), []);
  });

  test("a price of zero is refused", async () => {
    const user = start();
    await addItem(user, "Bread", "0.00");
    assert.equal(describedBy(priceBox()), "The price must be more than 0");
    assert.deepEqual(namesIn("Items"), []);
  });

  test("an item already on the bill is refused, whatever the case", async () => {
    const user = start();
    await addItem(user, "Pizza", "10");
    await addItem(user, "PIZZA", "12");
    assert.equal(describedBy(itemBox()), "PIZZA is already on the bill");
    assert.deepEqual(namesIn("Items"), ["Pizza"]);
    subtotal("10.00");
  });

  test("the errors go away once a valid item is added", async () => {
    const user = start();
    await addItem(user, "", "x");
    await addItem(user, "Cake", "7.9");
    assert.equal(describedBy(itemBox()), "");
    assert.equal(describedBy(priceBox()), "");
    assert.ok(screen.getByText("7.90"));
  });

  test("removing an item updates the subtotal", async () => {
    const user = start();
    await addItem(user, "Pizza", "12.50");
    await addItem(user, "Juice", "4.25");
    await user.click(screen.getByRole("button", { name: "Remove Pizza" }));
    assert.deepEqual(namesIn("Items"), ["Juice"]);
    subtotal("4.25");
  });

  test("cents add up without floating point errors", async () => {
    const user = start();
    await addItem(user, "A", "0.10");
    await addItem(user, "B", "0.20");
    await addItem(user, "C", "1234.56");
    subtotal("1234.86");
  });
});
