// The order-tracking API: a list without N+1, live status over Server-Sent Events that works
// across instances, a gauge of open streams, and a drain for rolling deploys.
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import type { Bus } from "./bus.ts";
import { formatComment, formatEvent, parseLastEventId } from "./events.ts";
import { STATUSES, type OrderStore, type Status, type StatusChange } from "./store.ts";

export interface AppOptions {
  store: OrderStore;
  bus: Bus;
  /** Milliseconds between heartbeat comments on each stream (default 15000). */
  heartbeatMs?: number;
  /** The reconnection delay sent to clients as `retry:` (default 3000). */
  retryMs?: number;
  /** How long `drain()` keeps serving after readiness turns 503 (default 5000). */
  drainDelayMs?: number;
}

export interface App {
  server: Server;
  /** Readiness turns 503 at once; after `drainDelayMs`, open streams end and the server closes. */
  drain(): Promise<void>;
}

export const MAX_IDS = 50;

function sendJson(response: ServerResponse, status: number, value: unknown, headers: Record<string, string> = {}): void {
  response.writeHead(status, { "Content-Type": "application/json", ...headers });
  response.end(JSON.stringify(value));
}

async function readBody(request: IncomingMessage): Promise<string> {
  let body = "";
  for await (const chunk of request) body += chunk;
  return body;
}

function channel(orderId: number): string {
  return `order:${orderId}`;
}

export function createApp(options: AppOptions): App {
  const { store, bus } = options;
  const heartbeatMs = options.heartbeatMs ?? 15_000;
  const retryMs = options.retryMs ?? 3_000;
  const drainDelayMs = options.drainDelayMs ?? 5_000;
  const streams = new Set<ServerResponse>();
  let eventsSent = 0;
  let draining = false;

  function listOrders(url: URL, response: ServerResponse): void {
    const raw = url.searchParams.get("ids");
    const parts = raw === null || raw === "" ? [] : raw.split(",");
    if (parts.length === 0 || parts.length > MAX_IDS || !parts.every((part) => /^[1-9]\d*$/.test(part))) {
      sendJson(response, 400, { error: `ids must be 1 to ${MAX_IDS} positive whole numbers` });
      return;
    }
    // Two queries for the whole page, whatever its size: the orders, then all their items.
    const orders = store.getMany(parts.map(Number));
    const items = store.itemsOfMany(orders.map((order) => order.id));
    sendJson(response, 200, { orders: orders.map((order) => ({ ...order, items: items.get(order.id) ?? [] })) });
  }

  async function changeStatus(orderId: number, request: IncomingMessage, response: ServerResponse): Promise<void> {
    const order = store.get(orderId);
    if (!order) {
      sendJson(response, 404, { error: "order not found" });
      return;
    }
    let status: unknown;
    try {
      status = (JSON.parse(await readBody(request)) as { status?: unknown }).status;
    } catch {
      status = undefined;
    }
    if (typeof status !== "string" || !(STATUSES as readonly string[]).includes(status)) {
      sendJson(response, 422, { error: `status must be one of ${STATUSES.join(", ")}` });
      return;
    }
    if (STATUSES.indexOf(status as Status) <= STATUSES.indexOf(order.status)) {
      sendJson(response, 409, { error: "status can only move forward", current: order.status });
      return;
    }
    const change = store.setStatus(orderId, status as Status);
    // Every instance, this one included, hears about the change from the bus: one path, no duplicates.
    bus.publish(channel(orderId), JSON.stringify(change));
    sendJson(response, 200, change);
  }

  function stream(orderId: number, request: IncomingMessage, response: ServerResponse): void {
    if (draining) {
      sendJson(response, 503, { error: "draining" }, { "Retry-After": "1" });
      return;
    }
    const order = store.get(orderId);
    if (!order) {
      sendJson(response, 404, { error: "order not found" });
      return;
    }
    response.writeHead(200, { "Content-Type": "text/event-stream", "Cache-Control": "no-cache" });
    response.write(`retry: ${retryMs}\n\n`);
    streams.add(response);
    let lastVersion = 0;
    const send = (change: StatusChange): void => {
      if (change.version <= lastVersion) return;
      lastVersion = change.version;
      eventsSent += 1;
      response.write(formatEvent({ id: change.version, event: "status", data: JSON.stringify(change) }));
    };
    // Subscribe before sending the first events: a change published meanwhile is then not lost,
    // and the version check drops it if the replay already sent it.
    const unsubscribe = bus.subscribe(channel(orderId), (message) => send(JSON.parse(message) as StatusChange));
    const since = parseLastEventId(request.headers["last-event-id"] as string | undefined);
    if (since === undefined) {
      send({ orderId, status: order.status, version: order.version });
    } else {
      lastVersion = since;
      for (const change of store.historyAfter(orderId, since)) send(change);
    }
    const heartbeat = setInterval(() => response.write(formatComment("ping")), heartbeatMs);
    response.on("close", () => {
      clearInterval(heartbeat);
      unsubscribe();
      streams.delete(response);
    });
  }

  function metrics(response: ServerResponse): void {
    response.writeHead(200, { "Content-Type": "text/plain; version=0.0.4" });
    response.end(
      [
        "# HELP sse_clients_open Event streams open on this instance.",
        "# TYPE sse_clients_open gauge",
        `sse_clients_open ${streams.size}`,
        "# HELP sse_events_sent_total Events written to event streams by this instance.",
        "# TYPE sse_events_sent_total counter",
        `sse_events_sent_total ${eventsSent}`,
        "",
      ].join("\n"),
    );
  }

  async function handle(request: IncomingMessage, response: ServerResponse): Promise<void> {
    const url = new URL(request.url ?? "/", "http://localhost");
    const method = request.method ?? "GET";
    const path = url.pathname;
    const events = /^\/api\/orders\/(\d+)\/events$/.exec(path);
    const status = /^\/api\/orders\/(\d+)\/status$/.exec(path);
    if (method === "GET" && path === "/healthz") sendJson(response, 200, { status: "ok" });
    else if (method === "GET" && path === "/readyz") {
      if (draining) sendJson(response, 503, { status: "draining" });
      else sendJson(response, 200, { status: "ready" });
    } else if (method === "GET" && path === "/metrics") metrics(response);
    else if (method === "GET" && path === "/api/orders") listOrders(url, response);
    else if (method === "GET" && events) stream(Number(events[1]), request, response);
    else if (method === "POST" && status) await changeStatus(Number(status[1]), request, response);
    else sendJson(response, 404, { error: "not found" });
  }

  const server = createServer((request, response) => {
    handle(request, response).catch(() => {
      if (!response.headersSent) sendJson(response, 500, { error: "internal error" });
      else response.end();
    });
  });

  let drained: Promise<void> | undefined;
  function drain(): Promise<void> {
    draining = true;
    drained ??= new Promise<void>((resolve) => {
      setTimeout(() => {
        // Streams never finish on their own: end them, and EventSource reconnects to another instance.
        for (const open of streams) open.end();
        server.close(() => resolve());
        server.closeIdleConnections();
      }, drainDelayMs);
    });
    return drained;
  }

  return { server, drain };
}

if (import.meta.main) {
  const { OrderStore } = await import("./store.ts");
  const { Bus } = await import("./bus.ts");
  const store = new OrderStore();
  store.add({ id: 1, customer: "Ana", items: [{ sku: "book-42", quantity: 1 }] });
  const app = createApp({ store, bus: new Bus() });
  process.on("SIGTERM", () => void app.drain().then(() => process.exit(0)));
  app.server.listen(3000, () => console.log("curl -N http://localhost:3000/api/orders/1/events"));
}
