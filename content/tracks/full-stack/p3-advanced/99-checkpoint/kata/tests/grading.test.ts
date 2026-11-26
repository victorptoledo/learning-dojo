import assert from "node:assert/strict";
import { get, type IncomingHttpHeaders, type IncomingMessage } from "node:http";
import type { AddressInfo } from "node:net";
import { test, type TestContext } from "node:test";
import { createApp, type App } from "../app.ts";
import { Bus } from "../bus.ts";
import { formatComment, formatEvent, parseLastEventId } from "../events.ts";
import { OrderStore } from "../store.ts";

const T = { timeout: 5000 };

function seed(): OrderStore {
  const store = new OrderStore();
  store.add({ id: 1, customer: "Ana", items: [{ sku: "book-42", quantity: 1 }] });
  store.add({ id: 2, customer: "Bo", items: [{ sku: "mug", quantity: 2 }, { sku: "tea", quantity: 3 }] });
  store.add({ id: 3, customer: "Cy", items: [] });
  return store;
}

interface Running {
  app: App;
  base: string;
}

async function start(
  t: TestContext,
  store: OrderStore,
  bus: Bus,
  extra: { heartbeatMs?: number; drainDelayMs?: number } = {},
): Promise<Running> {
  const app = createApp({ store, bus, heartbeatMs: extra.heartbeatMs ?? 60_000, drainDelayMs: extra.drainDelayMs ?? 50 });
  await new Promise<void>((resolve) => app.server.listen(0, "127.0.0.1", resolve));
  t.after(() => {
    app.server.closeAllConnections();
    app.server.close();
  });
  return { app, base: `http://127.0.0.1:${(app.server.address() as AddressInfo).port}` };
}

async function postStatus(base: string, id: number, status: unknown): Promise<Response> {
  return fetch(`${base}/api/orders/${id}/status`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Connection: "close" },
    body: JSON.stringify({ status }),
  });
}

interface Stream {
  status: number;
  headers: IncomingHttpHeaders;
  /** Everything received so far. */
  text(): string;
  /** The complete frames received so far (split on blank lines). */
  frames(): string[];
  /** Waits until `count` frames match `predicate`, or fails after `ms`. */
  waitFor(predicate: (frame: string) => boolean, count?: number, ms?: number): Promise<string[]>;
  ended: Promise<void>;
  close(): void;
}

function openStream(t: TestContext, url: string, headers: Record<string, string> = {}): Promise<Stream> {
  return new Promise((resolve, reject) => {
    const request = get(url, { headers: { Accept: "text/event-stream", ...headers } }, (response: IncomingMessage) => {
      let received = "";
      let resolveEnded: () => void = () => {};
      const ended = new Promise<void>((done) => (resolveEnded = done));
      response.setEncoding("utf8");
      response.on("data", (chunk: string) => (received += chunk));
      response.on("end", () => resolveEnded());
      response.on("close", () => resolveEnded());
      const frames = (): string[] => received.split("\n\n").slice(0, -1);
      resolve({
        status: response.statusCode ?? 0,
        headers: response.headers,
        text: () => received,
        frames,
        async waitFor(predicate, count = 1, ms = 1500) {
          const deadline = Date.now() + ms;
          while (Date.now() < deadline) {
            const found = frames().filter(predicate);
            if (found.length >= count) return found;
            await new Promise((r) => setTimeout(r, 10));
          }
          assert.fail(`waited ${ms} ms for ${count} matching frame(s); received:\n${received}`);
        },
        ended,
        close: () => request.destroy(),
      });
    });
    request.on("error", (error) => {
      if ((error as NodeJS.ErrnoException).code !== "ECONNRESET") reject(error);
    });
    t.after(() => request.destroy());
  });
}

const isStatus = (frame: string): boolean => frame.includes("event: status");

function versions(frames: string[]): number[] {
  return frames.filter(isStatus).map((frame) => Number(/^id: (\d+)$/m.exec(frame)?.[1]));
}

async function settle(ms = 30): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, ms));
}

async function metric(base: string, name: string): Promise<number> {
  const text = await (await fetch(`${base}/metrics`, { headers: { Connection: "close" } })).text();
  const line = text.split("\n").find((l) => l.startsWith(`${name} `));
  assert.ok(line, `no ${name} line in /metrics:\n${text}`);
  return Number(line.slice(name.length + 1));
}

// ---- events.ts --------------------------------------------------------------------------

test("formatEvent writes one data line per line, and leaves out a missing id or event", () => {
  assert.equal(formatEvent({ id: 3, event: "status", data: "{}" }), "id: 3\nevent: status\ndata: {}\n\n");
  assert.equal(formatEvent({ data: "one\ntwo\r\nthree" }), "data: one\ndata: two\ndata: three\n\n");
  assert.equal(formatEvent({ event: "reload", data: "" }), "event: reload\ndata: \n\n");
  assert.equal(formatComment("ping"), ": ping\n\n");
});

test("parseLastEventId accepts only whole numbers", () => {
  assert.equal(parseLastEventId("7"), 7);
  assert.equal(parseLastEventId("0"), 0);
  for (const bad of [undefined, "", "abc", "-1", "1.5", " 3", "3 ", "1e3"]) {
    assert.equal(parseLastEventId(bad), undefined, `header ${JSON.stringify(bad)}`);
  }
});

// ---- the list page ----------------------------------------------------------------------

test("the order list runs at most two queries, whatever the number of orders", T, async (t) => {
  const store = seed();
  const { base } = await start(t, store, new Bus());
  store.queries = 0;
  const response = await fetch(`${base}/api/orders?ids=3,9,1,2`);
  assert.equal(response.status, 200);
  const body = (await response.json()) as { orders: { id: number; items: unknown[] }[] };
  assert.deepEqual(body.orders.map((o) => o.id), [3, 1, 2], "in the order asked, unknown ids left out");
  assert.deepEqual(body.orders[2]?.items, [{ sku: "mug", quantity: 2 }, { sku: "tea", quantity: 3 }]);
  assert.ok(store.queries <= 2, `the page ran ${store.queries} queries: that is an N+1`);
});

test("the order list refuses missing, malformed and too many ids with 400", T, async (t) => {
  const { base } = await start(t, seed(), new Bus());
  const many = Array.from({ length: 51 }, (_, i) => i + 1).join(",");
  for (const query of ["", "?ids=", "?ids=a", "?ids=0", "?ids=1,,2", "?ids=-1", `?ids=${many}`]) {
    const response = await fetch(`${base}/api/orders${query}`);
    assert.equal(response.status, 400, `GET /api/orders${query}`);
    await response.body?.cancel();
  }
  const fifty = Array.from({ length: 50 }, (_, i) => i + 1).join(",");
  assert.equal((await fetch(`${base}/api/orders?ids=${fifty}`)).status, 200, "50 ids are allowed");
});

// ---- status changes ---------------------------------------------------------------------

test("a status only moves forward: 404, 422 and 409 before the change is saved", T, async (t) => {
  const store = seed();
  const { base } = await start(t, store, new Bus());
  assert.equal((await postStatus(base, 99, "packed")).status, 404);
  assert.equal((await postStatus(base, 1, "lost")).status, 422);
  assert.equal((await postStatus(base, 1, 42)).status, 422);
  const same = await postStatus(base, 1, "placed");
  assert.equal(same.status, 409);
  assert.equal(((await same.json()) as { current: string }).current, "placed");
  const shipped = await postStatus(base, 1, "shipped");
  assert.equal(shipped.status, 200);
  assert.deepEqual(await shipped.json(), { orderId: 1, status: "shipped", version: 2 });
  assert.equal((await postStatus(base, 1, "packed")).status, 409, "back from shipped to packed");
  assert.equal(store.get(1)?.version, 2);
});

// ---- the event stream -------------------------------------------------------------------

test("a new stream gets event-stream headers, a retry delay and the current state", T, async (t) => {
  const store = seed();
  const { base } = await start(t, store, new Bus());
  await postStatus(base, 2, "packed");
  const stream = await openStream(t, `${base}/api/orders/2/events`);
  assert.equal(stream.status, 200);
  assert.match(String(stream.headers["content-type"]), /^text\/event-stream/);
  assert.equal(stream.headers["cache-control"], "no-cache");
  const [snapshot] = await stream.waitFor(isStatus);
  assert.ok(stream.text().startsWith("retry: 3000\n\n"), "the first frame sets the reconnection delay");
  assert.equal(snapshot, `id: 2\nevent: status\ndata: ${JSON.stringify({ orderId: 2, status: "packed", version: 2 })}`);
});

test("a stream for an unknown order is a 404, not an empty stream", T, async (t) => {
  const { base } = await start(t, seed(), new Bus());
  const stream = await openStream(t, `${base}/api/orders/99/events`);
  assert.equal(stream.status, 404);
});

test("Last-Event-ID replays only the changes the client missed, oldest first", T, async (t) => {
  const store = seed();
  const { base } = await start(t, store, new Bus());
  await postStatus(base, 1, "packed");
  await postStatus(base, 1, "shipped");
  await postStatus(base, 1, "delivered");
  const stream = await openStream(t, `${base}/api/orders/1/events`, { "Last-Event-ID": "2" });
  await stream.waitFor(isStatus, 2);
  await settle();
  assert.deepEqual(versions(stream.frames()), [3, 4], "versions 3 and 4, and no snapshot on top");
  assert.match(stream.frames().filter(isStatus)[0] ?? "", /"status":"shipped"/);
});

test("each change reaches a client of the same instance exactly once", T, async (t) => {
  const { base } = await start(t, seed(), new Bus());
  const stream = await openStream(t, `${base}/api/orders/1/events`);
  await stream.waitFor(isStatus);
  await postStatus(base, 1, "packed");
  await postStatus(base, 1, "shipped");
  await stream.waitFor(isStatus, 3);
  await settle(50);
  assert.deepEqual(versions(stream.frames()), [1, 2, 3]);
});

test("a change saved on one instance reaches the clients of another", T, async (t) => {
  const store = seed();
  const bus = new Bus();
  const a = await start(t, store, bus);
  const b = await start(t, store, bus);
  const onB = await openStream(t, `${b.base}/api/orders/3/events`);
  await onB.waitFor(isStatus);
  await postStatus(a.base, 3, "packed");
  const frames = await onB.waitFor(isStatus, 2);
  assert.match(frames[1] ?? "", /^id: 2\nevent: status\ndata: .*"status":"packed"/);
});

test("a closed stream unsubscribes and stops its heartbeat", T, async (t) => {
  const bus = new Bus();
  const { base } = await start(t, seed(), bus, { heartbeatMs: 20 });
  const stream = await openStream(t, `${base}/api/orders/1/events`);
  await stream.waitFor((frame) => frame === ": ping", 2);
  assert.equal(bus.listenerCount("order:1"), 1, "one subscription per open stream");
  assert.equal(await metric(base, "sse_clients_open"), 1);
  stream.close();
  await settle(60);
  assert.equal(bus.listenerCount("order:1"), 0, "the subscription leaks after the client left");
  assert.equal(await metric(base, "sse_clients_open"), 0);
});

test("/metrics has a gauge of open streams and a counter of events sent", T, async (t) => {
  const { base } = await start(t, seed(), new Bus());
  const first = await openStream(t, `${base}/api/orders/1/events`);
  const second = await openStream(t, `${base}/api/orders/2/events`);
  await first.waitFor(isStatus);
  await second.waitFor(isStatus);
  await postStatus(base, 1, "packed");
  await first.waitFor(isStatus, 2);
  const response = await fetch(`${base}/metrics`, { headers: { Connection: "close" } });
  assert.match(String(response.headers.get("content-type")), /^text\/plain/);
  const text = await response.text();
  assert.match(text, /^# TYPE sse_clients_open gauge$/m);
  assert.match(text, /^# TYPE sse_events_sent_total counter$/m);
  assert.equal(await metric(base, "sse_clients_open"), 2);
  assert.equal(await metric(base, "sse_events_sent_total"), 3);
});

// ---- probes and draining ----------------------------------------------------------------

test("liveness and readiness: draining turns readiness and new streams to 503 at once", T, async (t) => {
  const { app, base } = await start(t, seed(), new Bus(), { drainDelayMs: 300 });
  assert.equal((await fetch(`${base}/healthz`)).status, 200);
  assert.equal((await fetch(`${base}/readyz`)).status, 200);
  const drained = app.drain();
  const ready = await fetch(`${base}/readyz`, { headers: { Connection: "close" } });
  assert.equal(ready.status, 503, "the load balancer must stop sending traffic before the server closes");
  await ready.body?.cancel();
  assert.equal((await fetch(`${base}/healthz`, { headers: { Connection: "close" } })).status, 200, "still alive while draining");
  const late = await openStream(t, `${base}/api/orders/1/events`, { Connection: "close" });
  assert.equal(late.status, 503);
  assert.equal(late.headers["retry-after"], "1");
  await drained;
});

test("drain ends the open streams, unsubscribes them and lets the server close", T, async (t) => {
  const bus = new Bus();
  const { app, base } = await start(t, seed(), bus, { drainDelayMs: 50 });
  const one = await openStream(t, `${base}/api/orders/1/events`);
  const two = await openStream(t, `${base}/api/orders/2/events`);
  await one.waitFor(isStatus);
  await two.waitFor(isStatus);
  const outcome = await Promise.race([
    app.drain().then(() => "closed"),
    new Promise((resolve) => setTimeout(() => resolve("still open"), 2000)),
  ]);
  assert.equal(outcome, "closed", "server.close() waits for every connection: open streams must be ended");
  await Promise.all([one.ended, two.ended]);
  assert.equal(bus.listenerCount("order:1") + bus.listenerCount("order:2"), 0);
});
