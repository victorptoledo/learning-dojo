import assert from "node:assert/strict";
import type { AddressInfo } from "node:net";
import { test } from "node:test";
import { createApp } from "../app.ts";
import { Bus } from "../bus.ts";
import { formatEvent } from "../events.ts";
import { OrderStore } from "../store.ts";

test("formats one status event", () => {
  assert.equal(formatEvent({ id: 2, event: "status", data: '{"version":2}' }), 'id: 2\nevent: status\ndata: {"version":2}\n\n');
});

test("the order list does not query once per order", async (t) => {
  const store = new OrderStore();
  store.add({ id: 1, customer: "Ana", items: [{ sku: "book-42", quantity: 1 }] });
  store.add({ id: 2, customer: "Bo", items: [] });
  const { server } = createApp({ store, bus: new Bus() });
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => server.close());
  store.queries = 0;
  const response = await fetch(`http://127.0.0.1:${(server.address() as AddressInfo).port}/api/orders?ids=2,1`, {
    headers: { Connection: "close" },
  });
  const body = (await response.json()) as { orders: { id: number }[] };
  assert.deepEqual(body.orders.map((order) => order.id), [2, 1]);
  assert.ok(store.queries <= 2, `${store.queries} queries for 2 orders`);
});
