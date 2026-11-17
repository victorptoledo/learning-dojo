import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { z } from "zod";
import { createActor, defineMachine } from "../machine.ts";

const order = defineMachine({
  initial: "open",
  states: {
    open: { on: { ADD: "open", PAY: "paid", CANCEL: "cancelled" } },
    paid: { on: { SHIP: "shipped", REFUND: "cancelled" } },
    shipped: {},
    cancelled: {},
  },
});

type Cart = { items: string[]; total: number; paid: number };

const shop = () =>
  createActor(order, {
    context: { items: [], total: 0, paid: 0 } as Cart,
    events: {
      ADD: z.object({ item: z.string().min(1), price: z.number().positive() }),
      PAY: z.object({ amount: z.number().positive(), currency: z.string().default("BRL") }),
    },
    guards: {
      "open.PAY": (context, payload) => payload.amount >= context.total && context.items.length > 0,
    },
    updates: {
      "open.ADD": (context, payload) => ({ ...context, items: [...context.items, payload.item], total: context.total + payload.price }),
      "open.PAY": (context, payload) => ({ ...context, paid: payload.amount }),
    },
  });

describe("an actor", () => {
  test("starts in the initial state with the context", () => {
    const actor = shop();
    assert.equal(actor.state, "open");
    assert.deepEqual(actor.context, { items: [], total: 0, paid: 0 });
  });

  test("moves on send and updates the context", () => {
    const actor = shop();
    assert.deepEqual(actor.send("ADD", { item: "book", price: 40 }), { ok: true, state: "open" });
    assert.deepEqual(actor.send("ADD", { item: "pen", price: 2.5 }), { ok: true, state: "open" });
    assert.deepEqual(actor.context, { items: ["book", "pen"], total: 42.5, paid: 0 });
    assert.deepEqual(actor.send("PAY", { amount: 50 }), { ok: true, state: "paid" });
    assert.equal(actor.state, "paid");
    assert.equal(actor.context.paid, 50);
    assert.deepEqual(actor.send("SHIP"), { ok: true, state: "shipped" });
    assert.equal(actor.state, "shipped");
  });

  test("an event the state does not accept is not-allowed and changes nothing", () => {
    const actor = shop();
    const before = actor.context;
    assert.deepEqual(actor.send("SHIP"), { ok: false, reason: "not-allowed" });
    assert.equal(actor.state, "open");
    assert.equal(actor.context, before);
  });

  test("a payload the schema rejects is invalid-payload", () => {
    const actor = shop();
    assert.deepEqual(actor.send("ADD", { item: "", price: 1 }), { ok: false, reason: "invalid-payload" });
    assert.deepEqual(actor.send("ADD", { item: "x", price: -1 }), { ok: false, reason: "invalid-payload" });
    assert.deepEqual(actor.context.items, []);
  });

  test("a guard returning false is guard and changes nothing", () => {
    const actor = shop();
    assert.deepEqual(actor.send("PAY", { amount: 10 }), { ok: false, reason: "guard" });
    actor.send("ADD", { item: "book", price: 40 });
    assert.deepEqual(actor.send("PAY", { amount: 10 }), { ok: false, reason: "guard" });
    assert.equal(actor.state, "open");
    assert.equal(actor.context.paid, 0);
  });

  test("not-allowed comes before the payload and the guard", () => {
    const actor = shop();
    actor.send("CANCEL");
    assert.deepEqual(actor.send("PAY", { amount: -1 }), { ok: false, reason: "not-allowed" });
  });

  test("the payload is checked before the guard", () => {
    let calls = 0;
    const actor = createActor(order, {
      context: {},
      events: { PAY: z.object({ amount: z.number() }) },
      guards: {
        "open.PAY": () => {
          calls += 1;
          return false;
        },
      },
    });
    const invalid = JSON.parse('{"amount":"1"}') as { amount: number };
    assert.deepEqual(actor.send("PAY", invalid), { ok: false, reason: "invalid-payload" });
    assert.equal(calls, 0);
  });

  test("guards and updates get the payload as the schema parsed it", () => {
    const seen: unknown[] = [];
    const actor = createActor(order, {
      context: { currency: "" },
      events: { PAY: z.object({ amount: z.number(), currency: z.string().default("BRL") }) },
      guards: {
        "open.PAY": (_, payload) => {
          seen.push(payload);
          return true;
        },
      },
      updates: { "open.PAY": (_, payload) => ({ currency: payload.currency }) },
    });
    const extra = { amount: 5, secret: "drop me" } as { amount: number };
    actor.send("PAY", extra);
    assert.deepEqual(seen, [{ amount: 5, currency: "BRL" }]);
    assert.deepEqual(actor.context, { currency: "BRL" });
  });

  test("without an update the context stays the same object", () => {
    const context = { n: 1 };
    const actor = createActor(order, { context });
    actor.send("PAY");
    assert.equal(actor.state, "paid");
    assert.equal(actor.context, context);
  });

  test("events without a schema have an undefined payload", () => {
    const seen: unknown[] = [];
    const actor = createActor(order, {
      context: 0,
      updates: {
        "open.CANCEL": (context, payload) => {
          seen.push(payload);
          return context + 1;
        },
      },
    });
    actor.send("CANCEL");
    assert.deepEqual(seen, [undefined]);
    assert.equal(actor.context, 1);
  });

  test("guards and updates are per transition", () => {
    const actor = createActor(order, {
      context: [] as string[],
      updates: {
        "open.CANCEL": (context) => [...context, "cancelled while open"],
        "paid.REFUND": (context) => [...context, "refunded"],
      },
    });
    actor.send("PAY");
    actor.send("REFUND");
    assert.deepEqual(actor.context, ["refunded"]);
    assert.equal(actor.state, "cancelled");
  });

  test("two actors are independent", () => {
    const first = shop();
    const second = shop();
    first.send("ADD", { item: "book", price: 40 });
    first.send("CANCEL");
    assert.equal(first.state, "cancelled");
    assert.equal(second.state, "open");
    assert.deepEqual(second.context.items, []);
  });

  test("names from outside the table are not-allowed", () => {
    const actor = createActor(order, { context: null });
    const event = "toString" as "PAY";
    assert.deepEqual(actor.send(event), { ok: false, reason: "not-allowed" });
  });
});
