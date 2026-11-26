// The orders and their status history: an in-memory stand-in for the database that every API
// instance shares. Written for you; every method that reads or writes counts as one query.
export const STATUSES = ["placed", "packed", "shipped", "delivered"] as const;
export type Status = (typeof STATUSES)[number];

export interface Item {
  sku: string;
  quantity: number;
}

export interface Order {
  id: number;
  customer: string;
  status: Status;
  version: number;
}

/** One step of an order's history; `version` grows by one with every change. */
export interface StatusChange {
  orderId: number;
  status: Status;
  version: number;
}

export class OrderStore {
  /** How many queries ran, as the database's statement counter would show. */
  queries = 0;
  readonly #orders = new Map<number, Order>();
  readonly #items = new Map<number, Item[]>();
  readonly #history = new Map<number, StatusChange[]>();

  /** Seeds an order at version 1, status "placed" (not counted as a query). */
  add(order: { id: number; customer: string; items: Item[] }): void {
    this.#orders.set(order.id, { id: order.id, customer: order.customer, status: "placed", version: 1 });
    this.#items.set(order.id, order.items.map((item) => ({ ...item })));
    this.#history.set(order.id, [{ orderId: order.id, status: "placed", version: 1 }]);
  }

  get(id: number): Order | undefined {
    this.queries += 1;
    const order = this.#orders.get(id);
    return order && { ...order };
  }

  /** `SELECT ... WHERE id IN (...)`: the orders found, in the order of `ids`. */
  getMany(ids: number[]): Order[] {
    this.queries += 1;
    return ids.flatMap((id) => {
      const order = this.#orders.get(id);
      return order ? [{ ...order }] : [];
    });
  }

  itemsOf(orderId: number): Item[] {
    this.queries += 1;
    return (this.#items.get(orderId) ?? []).map((item) => ({ ...item }));
  }

  /** `SELECT ... WHERE order_id IN (...)`: the items of every order, by order id. */
  itemsOfMany(orderIds: number[]): Map<number, Item[]> {
    this.queries += 1;
    return new Map(orderIds.map((id) => [id, (this.#items.get(id) ?? []).map((item) => ({ ...item }))]));
  }

  /** The changes with a version greater than `version`, oldest first. */
  historyAfter(orderId: number, version: number): StatusChange[] {
    this.queries += 1;
    return (this.#history.get(orderId) ?? []).filter((change) => change.version > version).map((change) => ({ ...change }));
  }

  /** Sets the status, bumps the version and records the change. The caller checks the rules. */
  setStatus(orderId: number, status: Status): StatusChange {
    this.queries += 1;
    const order = this.#orders.get(orderId);
    if (!order) throw new Error(`no order ${orderId}`);
    order.status = status;
    order.version += 1;
    const change = { orderId, status, version: order.version };
    this.#history.get(orderId)?.push(change);
    return { ...change };
  }
}
