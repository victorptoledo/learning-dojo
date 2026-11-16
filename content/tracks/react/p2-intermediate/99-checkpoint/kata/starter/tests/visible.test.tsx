import assert from "node:assert/strict";
import { test } from "node:test";
import { cartReducer, EMPTY_CART } from "../cart.ts";

test("adding a product creates a line with quantity 1", () => {
  const tea = { id: "tea", name: "Tea", price: 450 };
  const state = cartReducer(EMPTY_CART, { type: "add", product: tea });
  assert.deepEqual(state, { lines: [{ product: tea, quantity: 1 }] });
});
