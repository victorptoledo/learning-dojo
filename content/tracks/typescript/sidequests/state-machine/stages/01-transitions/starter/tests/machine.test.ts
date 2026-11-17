// Your own tests: `npm test` runs every tests/*.test.ts. Add more as you go.
import assert from "node:assert/strict";
import { test } from "node:test";
import { defineMachine } from "../machine.ts";

test("defineMachine keeps the initial state", () => {
  const light = defineMachine({ initial: "green", states: { green: { on: { NEXT: "red" } }, red: { on: { NEXT: "green" } } } });
  assert.equal(light.initial, "green");
});
