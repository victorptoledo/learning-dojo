// Orders, the events that change them, and a pipeline that applies an event to many orders.
import { canTransition, ORDER_STATUSES, type OrderStatus } from "./status.ts";

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
  return order.items.reduce((sum, item) => sum + item.quantity * item.priceCents, 0);
}

const TARGET = {
  pay: "paid",
  ship: "shipped",
  deliver: "delivered",
  cancel: "cancelled",
} as const satisfies Record<OrderEvent["type"], OrderStatus>;

/**
 * A new order after the event (the original is not changed): pay -> paid, ship -> shipped
 * with the tracking id, deliver -> delivered, cancel -> cancelled with the reason.
 * A TransitionError when TRANSITIONS does not allow it.
 */
export function apply(order: Order, event: OrderEvent): Order {
  const status = TARGET[event.type];
  if (!canTransition(order.status, status)) {
    throw new TransitionError(order, event);
  }
  switch (event.type) {
    case "ship":
      return { ...order, status, trackingId: event.trackingId };
    case "cancel":
      return { ...order, status, cancelReason: event.reason };
    case "pay":
    case "deliver":
      return { ...order, status };
  }
}

/** How many orders are in each status, with every status present (0 when none). */
export function summarize(orders: readonly Order[]): Record<OrderStatus, number> {
  const counts = Object.fromEntries(ORDER_STATUSES.map((status) => [status, 0])) as Record<
    OrderStatus,
    number
  >;
  for (const order of orders) {
    counts[order.status] += 1;
  }
  return counts;
}

export interface ProcessResult {
  /** The orders after the event, in the order of the ids that succeeded. */
  updated: Order[];
  /** The ids that failed (loading or applying), in the order of the ids, with the error. */
  failed: { id: string; error: Error }[];
}

type Outcome = { id: string; ok: true; order: Order } | { id: string; ok: false; error: Error };

function toError(value: unknown): Error {
  return value instanceof Error ? value : new Error(String(value));
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
  const outcomes = new Array<Outcome>(ids.length);
  let next = 0;
  const worker = async () => {
    while (next < ids.length) {
      const index = next;
      next += 1;
      const id = ids[index] as string;
      try {
        outcomes[index] = { id, ok: true, order: apply(await load(id), event) };
      } catch (error) {
        outcomes[index] = { id, ok: false, error: toError(error) };
      }
    }
  };
  await Promise.all(Array.from({ length: Math.min(concurrency, ids.length) }, worker));

  const result: ProcessResult = { updated: [], failed: [] };
  for (const outcome of outcomes) {
    if (outcome.ok) {
      result.updated.push(outcome.order);
    } else {
      result.failed.push({ id: outcome.id, error: outcome.error });
    }
  }
  return result;
}
