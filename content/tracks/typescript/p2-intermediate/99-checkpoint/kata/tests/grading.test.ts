import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { apply, processAll, summarize, total, TransitionError, type Order } from "../orders.ts";
import { canTransition, isOrderStatus } from "../status.ts";

const order = (id: string, status: Order["status"] = "pending"): Order => ({
  id,
  customer: "Ada",
  status,
  items: [
    { sku: "pen", quantity: 3, priceCents: 150 },
    { sku: "pad", quantity: 1, priceCents: 899 },
  ],
});

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

describe("status", () => {
  test("isOrderStatus accepts only the known statuses", () => {
    assert.equal(isOrderStatus("shipped"), true);
    for (const value of ["lost", "", "PAID", 1, null, undefined, {}]) {
      assert.equal(isOrderStatus(value), false, String(value));
    }
  });

  test("canTransition follows the table", () => {
    assert.equal(canTransition("pending", "paid"), true);
    assert.equal(canTransition("pending", "cancelled"), true);
    assert.equal(canTransition("paid", "shipped"), true);
    assert.equal(canTransition("paid", "cancelled"), true);
    assert.equal(canTransition("shipped", "delivered"), true);
    assert.equal(canTransition("pending", "shipped"), false);
    assert.equal(canTransition("shipped", "cancelled"), false);
    assert.equal(canTransition("delivered", "pending"), false);
    assert.equal(canTransition("cancelled", "paid"), false);
  });
});

describe("orders", () => {
  test("total sums quantity times price", () => {
    assert.equal(total(order("o1")), 3 * 150 + 899);
    assert.equal(total({ ...order("o2"), items: [] }), 0);
  });

  test("apply walks the whole happy path without changing the original", () => {
    const start = order("o1");
    const paid = apply(start, { type: "pay" });
    const shipped = apply(paid, { type: "ship", trackingId: "TRK-1" });
    const delivered = apply(shipped, { type: "deliver" });
    assert.equal(start.status, "pending");
    assert.equal(paid.status, "paid");
    assert.equal(shipped.status, "shipped");
    assert.equal(shipped.trackingId, "TRK-1");
    assert.equal(delivered.status, "delivered");
    assert.equal(delivered.trackingId, "TRK-1");
    assert.deepEqual(delivered.items, start.items);
  });

  test("apply cancels with a reason", () => {
    const cancelled = apply(order("o1", "paid"), { type: "cancel", reason: "out of stock" });
    assert.equal(cancelled.status, "cancelled");
    assert.equal(cancelled.cancelReason, "out of stock");
  });

  test("apply throws a TransitionError for a move the table forbids", () => {
    assert.throws(
      () => apply(order("o9", "delivered"), { type: "cancel", reason: "late" }),
      (error: unknown) => {
        assert.ok(error instanceof TransitionError);
        assert.equal(error.message, "cannot cancel order o9 in status delivered");
        assert.equal(error.from, "delivered");
        assert.equal(error.event, "cancel");
        return true;
      },
    );
    assert.throws(() => apply(order("o1"), { type: "deliver" }), TransitionError);
  });

  test("summarize counts every status", () => {
    assert.deepEqual(summarize([order("a"), order("b", "paid"), order("c")]), {
      pending: 2,
      paid: 1,
      shipped: 0,
      delivered: 0,
      cancelled: 0,
    });
    assert.deepEqual(summarize([]), {
      pending: 0,
      paid: 0,
      shipped: 0,
      delivered: 0,
      cancelled: 0,
    });
  });
});

describe("processAll", () => {
  const store = new Map<string, Order>([
    ["a", order("a")],
    ["b", order("b", "shipped")],
    ["c", order("c")],
    ["d", order("d")],
  ]);
  const load = async (id: string): Promise<Order> => {
    await wait(id === "a" ? 20 : 2);
    const found = store.get(id);
    if (!found) {
      throw new Error(`order ${id} not found`);
    }
    return found;
  };

  test("collects successes and failures in the order of the ids", async () => {
    const result = await processAll(["a", "b", "x", "c"], load, { type: "pay" });
    assert.deepEqual(
      result.updated.map((each) => [each.id, each.status]),
      [
        ["a", "paid"],
        ["c", "paid"],
      ],
    );
    assert.deepEqual(
      result.failed.map(({ id, error }) => [id, error.message]),
      [
        ["b", "cannot pay order b in status shipped"],
        ["x", "order x not found"],
      ],
    );
    assert.ok(result.failed[0]?.error instanceof TransitionError);
  });

  test("turns thrown non-errors into Errors", async () => {
    const result = await processAll(["z"], () => Promise.reject("gone"), { type: "pay" });
    assert.equal(result.failed[0]?.error.message, "gone");
    assert.ok(result.failed[0]?.error instanceof Error);
  });

  test("limits the concurrent loads", async () => {
    let running = 0;
    let peak = 0;
    const counted = async (id: string) => {
      running += 1;
      peak = Math.max(peak, running);
      try {
        return await load(id);
      } finally {
        running -= 1;
      }
    };
    await processAll(["a", "b", "c", "d", "a", "c"], counted, { type: "pay" }, { concurrency: 2 });
    assert.equal(peak, 2);
  });
});
