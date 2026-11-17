import { z } from "zod";
import { createActor, defineMachine, type SendResult, type StateOf } from "../machine.ts";
import type { Equal, Expect } from "./expect.ts";

const order = defineMachine({
  initial: "open",
  states: {
    open: { on: { PAY: "paid", CANCEL: "cancelled" } },
    paid: { on: { SHIP: "shipped" } },
    shipped: {},
    cancelled: {},
  },
});
type Order = typeof order;

const actor = createActor(order, {
  context: { paid: 0 },
  events: { PAY: z.object({ amount: z.number(), note: z.string().optional() }) },
  guards: { "open.PAY": (_context, payload) => payload.amount > 0 },
  updates: { "open.PAY": (context, payload) => ({ paid: context.paid + payload.amount }) },
});

// test: state and context are typed
export type State = Expect<Equal<typeof actor.state, StateOf<Order>>>;
export type Context = Expect<Equal<typeof actor.context, { paid: number }>>;

// test: send returns a SendResult
const result = actor.send("CANCEL");
export type Result = Expect<Equal<typeof result, SendResult<Order>>>;
export type Reasons = Expect<Equal<Extract<SendResult<Order>, { ok: false }>["reason"], "not-allowed" | "invalid-payload" | "guard">>;
export type NewState = Expect<Equal<Extract<SendResult<Order>, { ok: true }>["state"], StateOf<Order>>>;

// test: an event with a schema needs its payload
actor.send("PAY", { amount: 1 });
actor.send("PAY", { amount: 1, note: "gift" });
// @ts-expect-error: PAY needs a payload
actor.send("PAY");
// @ts-expect-error: amount must be a number
actor.send("PAY", { amount: "1" });

// test: an event without a schema takes no payload
actor.send("SHIP");
// @ts-expect-error: CANCEL has no payload
actor.send("CANCEL", { reason: "late" });

// test: only events of the machine can be sent
// @ts-expect-error: REFUND is not an event of the machine
actor.send("REFUND");

// test: guards and updates see the typed context and payload
createActor(order, {
  context: { paid: 0 },
  events: { PAY: z.object({ amount: z.number() }) },
  updates: {
    "open.PAY": (context, payload) => {
      const paid: number = context.paid;
      const amount: number = payload.amount;
      return { paid: paid + amount };
    },
    "open.CANCEL": (context, payload) => {
      const nothing: undefined = payload;
      void nothing;
      return context;
    },
  },
});

// test: an update must return the context type
// @ts-expect-error: paid must stay a number
createActor(order, { context: { paid: 0 }, updates: { "open.CANCEL": () => ({ paid: "none" }) } });

// test: a guard must return a boolean
// @ts-expect-error: a guard returns boolean
createActor(order, { context: 0, guards: { "open.CANCEL": () => "yes" } });

// test: keys are transitions of the machine
// @ts-expect-error: shipped has no CANCEL
createActor(order, { context: 0, guards: { "shipped.CANCEL": () => true } });
// @ts-expect-error: "open.SHIP" is not a transition
createActor(order, { context: 0, updates: { "open.SHIP": (context) => context } });

// test: events names events of the machine
// @ts-expect-error: REFUND is not an event of the machine
createActor(order, { context: 0, events: { REFUND: z.object({}) } });
