import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { z } from "zod";
import { createActor, defineMachine, restoreActor } from "../machine.ts";

const ticket = defineMachine({
  initial: "new",
  states: {
    new: { on: { ASSIGN: "assigned", CLOSE: "closed" } },
    assigned: { on: { ASSIGN: "assigned", RESOLVE: "resolved", CLOSE: "closed" } },
    resolved: { on: { REOPEN: "assigned", CLOSE: "closed" } },
    closed: {},
  },
});

const contextSchema = z.object({ assignee: z.string().nullable(), reopened: z.number().int().min(0) });
type Ticket = z.infer<typeof contextSchema>;

const behaviour = {
  events: { ASSIGN: z.object({ to: z.string().min(1) }) },
  guards: { "resolved.REOPEN": (context: Ticket) => context.reopened < 2 },
  updates: {
    "new.ASSIGN": (context: Ticket, payload: { to: string }) => ({ ...context, assignee: payload.to }),
    "assigned.ASSIGN": (context: Ticket, payload: { to: string }) => ({ ...context, assignee: payload.to }),
    "resolved.REOPEN": (context: Ticket) => ({ ...context, reopened: context.reopened + 1 }),
  },
};

const fresh = () => createActor(ticket, { context: { assignee: null, reopened: 0 } as Ticket, ...behaviour });

describe("receive", () => {
  test("acts like send for a valid event", () => {
    const actor = fresh();
    assert.deepEqual(actor.receive({ type: "ASSIGN", payload: { to: "ana" } }), { ok: true, state: "assigned" });
    assert.equal(actor.context.assignee, "ana");
    assert.deepEqual(actor.receive(JSON.parse('{"type":"RESOLVE"}')), { ok: true, state: "resolved" });
  });

  test("anything that is not an object with a string type is invalid-event", () => {
    const actor = fresh();
    for (const input of [null, undefined, "ASSIGN", 42, ["ASSIGN"], {}, { type: 1 }, { kind: "ASSIGN" }]) {
      assert.deepEqual(actor.receive(input), { ok: false, reason: "invalid-event" }, JSON.stringify(input));
    }
    assert.equal(actor.state, "new");
  });

  test("a type that is no event of the machine is unknown-event", () => {
    const actor = fresh();
    assert.deepEqual(actor.receive({ type: "DELETE" }), { ok: false, reason: "unknown-event" });
    assert.deepEqual(actor.receive({ type: "toString" }), { ok: false, reason: "unknown-event" });
  });

  test("an event of another state is not-allowed", () => {
    assert.deepEqual(fresh().receive({ type: "REOPEN" }), { ok: false, reason: "not-allowed" });
  });

  test("the payload goes through the schema", () => {
    const actor = fresh();
    assert.deepEqual(actor.receive({ type: "ASSIGN" }), { ok: false, reason: "invalid-payload" });
    assert.deepEqual(actor.receive({ type: "ASSIGN", payload: { to: 7 } }), { ok: false, reason: "invalid-payload" });
    assert.equal(actor.state, "new");
  });

  test("an event without a schema ignores the payload", () => {
    assert.deepEqual(fresh().receive({ type: "CLOSE", payload: { why: "spam" } }), { ok: true, state: "closed" });
  });
});

describe("history", () => {
  test("lists the transitions, oldest first, from send and receive", () => {
    const actor = fresh();
    assert.deepEqual(actor.history, []);
    actor.send("ASSIGN", { to: "ana" });
    actor.receive({ type: "ASSIGN", payload: { to: "bia" } });
    actor.send("RESOLVE");
    assert.deepEqual(actor.history, [
      { from: "new", event: "ASSIGN", to: "assigned" },
      { from: "assigned", event: "ASSIGN", to: "assigned" },
      { from: "assigned", event: "RESOLVE", to: "resolved" },
    ]);
  });

  test("failed sends leave no step", () => {
    const actor = fresh();
    actor.send("RESOLVE");
    actor.receive({ type: "nope" });
    actor.send("ASSIGN", { to: "" });
    assert.deepEqual(actor.history, []);
  });

  test("guards that say no leave no step", () => {
    const actor = fresh();
    actor.send("ASSIGN", { to: "ana" });
    for (let round = 0; round < 3; round += 1) {
      actor.send("RESOLVE");
      actor.send("REOPEN");
    }
    assert.equal(actor.context.reopened, 2);
    assert.equal(actor.state, "resolved");
    assert.equal(actor.history.length, 6);
  });
});

describe("saving and restoring", () => {
  test("toJSON gives the state and the context", () => {
    const actor = fresh();
    actor.send("ASSIGN", { to: "ana" });
    assert.deepEqual(actor.toJSON(), { state: "assigned", context: { assignee: "ana", reopened: 0 } });
    assert.deepEqual(JSON.parse(JSON.stringify(actor)), { state: "assigned", context: { assignee: "ana", reopened: 0 } });
  });

  test("restoreActor brings it back through JSON, and it keeps working", () => {
    const actor = fresh();
    actor.send("ASSIGN", { to: "ana" });
    actor.send("RESOLVE");
    const restored = restoreActor(ticket, { contextSchema, ...behaviour }, JSON.parse(JSON.stringify(actor)));
    assert.equal(restored.ok, true);
    if (!restored.ok) {
      return;
    }
    const back = restored.actor;
    assert.equal(back.state, "resolved");
    assert.deepEqual(back.context, { assignee: "ana", reopened: 0 });
    assert.deepEqual(back.history, []);
    assert.deepEqual(back.send("REOPEN"), { ok: true, state: "assigned" });
    assert.equal(back.context.reopened, 1);
    assert.deepEqual(back.send("ASSIGN", { to: "" }), { ok: false, reason: "invalid-payload" });
    assert.equal(actor.state, "resolved");
  });

  test("the context is the one the schema parsed", () => {
    const schema = z.object({ tags: z.array(z.string()).default([]) });
    const restored = restoreActor(ticket, { contextSchema: schema }, { state: "new", context: { extra: 1 } });
    assert.deepEqual(restored.ok && restored.actor.context, { tags: [] });
  });

  test("rejects what is not a saved actor", () => {
    for (const saved of [null, "assigned", [], {}, { state: 3, context: {} }, { context: {} }]) {
      assert.deepEqual(restoreActor(ticket, { contextSchema }, saved), { ok: false, error: "not a saved actor" }, JSON.stringify(saved));
    }
  });

  test("rejects a state the machine does not have", () => {
    assert.deepEqual(restoreActor(ticket, { contextSchema }, { state: "archived", context: { assignee: null, reopened: 0 } }), {
      ok: false,
      error: "unknown state archived",
    });
    assert.deepEqual(restoreActor(ticket, { contextSchema }, { state: "toString", context: {} }), {
      ok: false,
      error: "unknown state toString",
    });
  });

  test("rejects a context the schema rejects", () => {
    assert.deepEqual(restoreActor(ticket, { contextSchema }, { state: "new", context: { assignee: 1, reopened: 0 } }), {
      ok: false,
      error: "invalid context",
    });
    assert.deepEqual(restoreActor(ticket, { contextSchema }, { state: "new" }), { ok: false, error: "invalid context" });
  });
});
