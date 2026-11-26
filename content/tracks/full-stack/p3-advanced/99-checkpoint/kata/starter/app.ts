// The order-tracking API. Today it only lists orders (with an N+1) and answers the liveness
// probe. Make it production-ready: see README.md.
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import type { Bus } from "./bus.ts";
import type { OrderStore } from "./store.ts";

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

export function createApp(options: AppOptions): App {
  const { store } = options;

  function listOrders(url: URL, response: ServerResponse): void {
    const ids = (url.searchParams.get("ids") ?? "").split(",").map(Number);
    const orders = [];
    for (const id of ids) {
      const order = store.get(id);
      if (order) orders.push({ ...order, items: store.itemsOf(id) });
    }
    sendJson(response, 200, { orders });
  }

  async function handle(request: IncomingMessage, response: ServerResponse): Promise<void> {
    const url = new URL(request.url ?? "/", "http://localhost");
    const method = request.method ?? "GET";
    const path = url.pathname;
    if (method === "GET" && path === "/healthz") sendJson(response, 200, { status: "ok" });
    else if (method === "GET" && path === "/api/orders") listOrders(url, response);
    // todo: /readyz, /metrics, POST /api/orders/:id/status, GET /api/orders/:id/events
    else sendJson(response, 404, { error: "not found" });
  }

  const server = createServer((request, response) => {
    handle(request, response).catch(() => {
      if (!response.headersSent) sendJson(response, 500, { error: "internal error" });
      else response.end();
    });
  });

  function drain(): Promise<void> {
    throw new Error("todo: turn readiness to 503, wait drainDelayMs, end the streams and close the server");
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
