import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { can, defineMachine, finalStates, reachable, transition, TransitionError } from "../machine.ts";

const order = defineMachine({
  initial: "open",
  states: {
    open: { on: { PAY: "paid", CANCEL: "cancelled" } },
    paid: { on: { SHIP: "shipped", REFUND: "cancelled" } },
    shipped: { on: { DELIVER: "delivered", LOSE: "lost" } },
    delivered: {},
    cancelled: { on: {} },
    lost: { on: { FIND: "shipped" } },
    archived: {},
  },
});

describe("defineMachine", () => {
  test("returns the initial state and the states as given", () => {
    const states = { a: { on: { GO: "b" } }, b: {} } as const;
    const machine = defineMachine({ initial: "a", states });
    assert.equal(machine.initial, "a");
    assert.equal(machine.states, states);
  });
});

describe("transition", () => {
  test("follows the table", () => {
    assert.equal(transition(order, "open", "PAY"), "paid");
    assert.equal(transition(order, "open", "CANCEL"), "cancelled");
    assert.equal(transition(order, "paid", "SHIP"), "shipped");
    assert.equal(transition(order, "lost", "FIND"), "shipped");
  });

  test("throws a TransitionError for data the compiler did not see", () => {
    const fromOutside = JSON.parse('{"state":"shipped","event":"CANCEL"}') as { state: "open"; event: "PAY" };
    assert.throws(
      () => transition(order, fromOutside.state, fromOutside.event),
      (error: unknown) =>
        error instanceof TransitionError &&
        error instanceof Error &&
        error.name === "TransitionError" &&
        error.state === "shipped" &&
        error.event === "CANCEL" &&
        error.message === "cannot CANCEL in shipped",
    );
  });

  test("an unknown state throws too", () => {
    const state = "nowhere" as "open";
    assert.throws(() => transition(order, state, "PAY"), TransitionError);
  });
});

describe("can", () => {
  test("tells which transitions exist", () => {
    assert.equal(can(order, "open", "PAY"), true);
    assert.equal(can(order, "paid", "PAY"), false);
    assert.equal(can(order, "delivered", "PAY"), false);
    assert.equal(can(order, "nowhere", "PAY"), false);
    assert.equal(can(order, "open", ""), false);
  });

  test("only own properties count", () => {
    assert.equal(can(order, "open", "toString"), false);
    assert.equal(can(order, "toString", "PAY"), false);
    assert.equal(can(order, "constructor", "constructor"), false);
  });
});

describe("finalStates", () => {
  test("are the states without events, in order", () => {
    assert.deepEqual(finalStates(order), ["delivered", "cancelled", "archived"]);
  });

  test("a cycle has none", () => {
    const light = defineMachine({ initial: "green", states: { green: { on: { NEXT: "red" } }, red: { on: { NEXT: "green" } } } });
    assert.deepEqual(finalStates(light), []);
  });
});

describe("reachable", () => {
  test("is breadth-first from initial, events in written order", () => {
    assert.deepEqual(reachable(order), ["open", "paid", "cancelled", "shipped", "delivered", "lost"]);
  });

  test("handles cycles and starts anywhere", () => {
    const loop = defineMachine({
      initial: "b",
      states: { a: { on: { X: "b" } }, b: { on: { Y: "c", Z: "a" } }, c: { on: { W: "b" } }, d: { on: { V: "a" } } },
    });
    assert.deepEqual(reachable(loop), ["b", "c", "a"]);
  });

  test("a machine of one state reaches itself", () => {
    assert.deepEqual(reachable(defineMachine({ initial: "only", states: { only: {} } })), ["only"]);
  });
});
