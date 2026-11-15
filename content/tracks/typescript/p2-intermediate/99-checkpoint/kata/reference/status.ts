// The order statuses and the moves allowed between them.

export const ORDER_STATUSES = ["pending", "paid", "shipped", "delivered", "cancelled"] as const;

/** The union "pending" | "paid" | ..., derived from ORDER_STATUSES. */
export type OrderStatus = (typeof ORDER_STATUSES)[number];

/** For each status, the statuses it may move to. */
export const TRANSITIONS = {
  pending: ["paid", "cancelled"],
  paid: ["shipped", "cancelled"],
  shipped: ["delivered"],
  delivered: [],
  cancelled: [],
} as const satisfies Record<OrderStatus, readonly OrderStatus[]>;

/** Narrows an unknown value to an OrderStatus. */
export function isOrderStatus(value: unknown): value is OrderStatus {
  return (ORDER_STATUSES as readonly unknown[]).includes(value);
}

/** Whether TRANSITIONS allows moving from `from` to `to`. */
export function canTransition(from: OrderStatus, to: OrderStatus): boolean {
  const allowed: readonly OrderStatus[] = TRANSITIONS[from];
  return allowed.includes(to);
}
