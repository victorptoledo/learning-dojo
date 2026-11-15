import assert from "node:assert/strict";
import { test } from "node:test";
import { apply, type Order } from "../orders.ts";

test("paying a pending order makes it paid", () => {
  const order: Order = { id: "o1", customer: "Ada", status: "pending", items: [] };
  assert.equal(apply(order, { type: "pay" }).status, "paid");
});
