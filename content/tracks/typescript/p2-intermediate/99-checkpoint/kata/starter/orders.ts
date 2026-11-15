// Orders, the events that change them, and a pipeline that applies an event to many orders.
import type { OrderStatus } from "./status.ts";

export interface LineItem {
  readonly sku: string;
  readonly quantity: number;
  readonly priceCents: number;
}

export interface Order {
  readonly id: string;
  readonly customer: string;
  readonly status: OrderStatus;
  readonly items: readonly LineItem[];
  readonly trackingId?: string;
  readonly cancelReason?: string;
}

export type OrderEvent =
  | { type: "pay" }
  | { type: "ship"; trackingId: string }
  | { type: "deliver" }
  | { type: "cancel"; reason: string };

/** Thrown by apply when the event is not allowed in the order's status. */
export class TransitionError extends Error {
  override name = "TransitionError";
  readonly orderId: string;
  readonly from: OrderStatus;
  readonly event: OrderEvent["type"];

  constructor(order: Order, event: OrderEvent) {
    super(`cannot ${event.type} order ${order.id} in status ${order.status}`);
    this.orderId = order.id;
    this.from = order.status;
    this.event = event.type;
  }
}

/** The total of the order in cents: quantity times price, summed. */
export function total(order: Order): number {
  throw new Error("todo: write total()");
}

/**
 * A new order after the event (the original is not changed): pay -> paid, ship -> shipped
 * with the tracking id, deliver -> delivered, cancel -> cancelled with the reason.
 * A TransitionError when TRANSITIONS does not allow it.
 */
export function apply(order: Order, event: OrderEvent): Order {
  throw new Error("todo: write apply()");
}

/** How many orders are in each status, with every status present (0 when none). */
export function summarize(orders: readonly Order[]): Record<OrderStatus, number> {
  throw new Error("todo: write summarize()");
}

export interface ProcessResult {
  /** The orders after the event, in the order of the ids that succeeded. */
  updated: Order[];
  /** The ids that failed (loading or applying), in the order of the ids, with the error. */
  failed: { id: string; error: Error }[];
}

/**
 * Loads each order, applies the event and collects what succeeded and what failed, with at
 * most `concurrency` loads running at a time (default 4). One failure never stops the others.
 */
export async function processAll(
  ids: readonly string[],
  load: (id: string) => Promise<Order>,
  event: OrderEvent,
  { concurrency = 4 }: { concurrency?: number } = {},
): Promise<ProcessResult> {
  throw new Error("todo: write processAll()");
}
