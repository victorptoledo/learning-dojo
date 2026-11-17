import {
  defineMachine,
  finalStates,
  reachable,
  transition,
  type EventOf,
  type EventsOf,
  type Next,
  type StateOf,
} from "../machine.ts";
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

// test: StateOf lists every state
export type States = Expect<Equal<StateOf<Order>, "open" | "paid" | "shipped" | "cancelled">>;

// test: EventsOf lists the events of one state
export type OpenEvents = Expect<Equal<EventsOf<Order, "open">, "PAY" | "CANCEL">>;
export type PaidEvents = Expect<Equal<EventsOf<Order, "paid">, "SHIP">>;
export type FinalEvents = Expect<Equal<EventsOf<Order, "shipped">, never>>;

// test: EventOf lists every event
export type Events = Expect<Equal<EventOf<Order>, "PAY" | "CANCEL" | "SHIP">>;

// test: Next is the literal target
export type AfterPay = Expect<Equal<Next<Order, "open", "PAY">, "paid">>;
export type AfterCancel = Expect<Equal<Next<Order, "open", "CANCEL">, "cancelled">>;

// test: transition returns the literal target
const paid = transition(order, "open", "PAY");
export type Paid = Expect<Equal<typeof paid, "paid">>;
const shipped = transition(order, paid, "SHIP");
export type Shipped = Expect<Equal<typeof shipped, "shipped">>;

// test: an event the state does not accept does not compile
// @ts-expect-error: shipped accepts no event
transition(order, "shipped", "CANCEL");
// @ts-expect-error: paid does not accept PAY
transition(order, "paid", "PAY");

// test: an unknown state or event does not compile
// @ts-expect-error: "closed" is not a state
transition(order, "closed", "PAY");
// @ts-expect-error: "PAYY" is not an event
transition(order, "open", "PAYY");

// test: a target must be a state
// @ts-expect-error: "payd" is not a state
defineMachine({ initial: "open", states: { open: { on: { PAY: "payd" } }, paid: {} } });

// test: initial must be a state
// @ts-expect-error: "start" is not a state
defineMachine({ initial: "start", states: { open: {} } });

// test: finalStates and reachable return state names
export type Finals = Expect<Equal<ReturnType<typeof finalStates<Order>>, StateOf<Order>[]>>;
export type Reached = Expect<Equal<ReturnType<typeof reachable<Order>>, StateOf<Order>[]>>;
