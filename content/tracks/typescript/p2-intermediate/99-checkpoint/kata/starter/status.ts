// The order statuses and the moves allowed between them.

export const ORDER_STATUSES = ["pending", "paid", "shipped", "delivered", "cancelled"] as const;

/** todo: derive the union "pending" | "paid" | ... from ORDER_STATUSES instead of string. */
export type OrderStatus = string;

/**
 * todo: for each status, the statuses it may move to (pending: paid or cancelled; paid:
 * shipped or cancelled; shipped: delivered; delivered and cancelled: none). Keep the literal
 * types (`as const`) and check the shape with `satisfies`.
 */
export const TRANSITIONS: Record<string, readonly string[]> = {};

/** Narrows an unknown value to an OrderStatus. */
export function isOrderStatus(value: unknown): value is OrderStatus {
  throw new Error("todo: write isOrderStatus()");
}

/** Whether TRANSITIONS allows moving from `from` to `to`. */
export function canTransition(from: OrderStatus, to: OrderStatus): boolean {
  throw new Error("todo: write canTransition()");
}
