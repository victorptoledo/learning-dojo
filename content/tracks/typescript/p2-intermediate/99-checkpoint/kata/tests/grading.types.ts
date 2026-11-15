import type { Order, OrderEvent } from "../orders.ts";
import { apply } from "../orders.ts";
import type { OrderStatus } from "../status.ts";
import { TRANSITIONS } from "../status.ts";
import type { Equal, Expect } from "./expect.ts";

// test: OrderStatus is the union of the statuses
export type Statuses = Expect<
  Equal<OrderStatus, "pending" | "paid" | "shipped" | "delivered" | "cancelled">
>;
// @ts-expect-error: "lost" is not a status
export const lost: OrderStatus = "lost";

// test: TRANSITIONS keeps its literal types
export type FromPending = Expect<Equal<(typeof TRANSITIONS)["pending"], readonly ["paid", "cancelled"]>>;
export type FromDelivered = Expect<Equal<(typeof TRANSITIONS)["delivered"], readonly []>>;

// test: events carry their own data
declare const order: Order;
// @ts-expect-error: shipping needs a tracking id
apply(order, { type: "ship" });
export type EventTypes = Expect<Equal<OrderEvent["type"], "pay" | "ship" | "deliver" | "cancel">>;
