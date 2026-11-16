// The orders API, instrumented: one trace per request, one log line, one histogram observation.
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import { LatencyHistogram } from "./metrics.ts";
import { formatTraceparent, newSpanId, newTraceId, parseTraceparent, type SpanContext } from "./trace.ts";

export interface LogLine {
  level: "info" | "error";
  msg: string;
  [field: string]: unknown;
}

export interface AppOptions {
  inventoryUrl: string;
  log: (line: LogLine) => void;
  /** Milliseconds; Date.now in production, a fake clock in tests. */
  now: () => number;
  fetchFn?: typeof fetch;
}

/** What a route handler gets: the request, the span it runs in, and helpers. */
export interface RouteContext {
  request: IncomingMessage;
  span: SpanContext;
  params: Record<string, string>;
}

export interface RouteAnswer {
  status: number;
  body: string;
  contentType?: string;
}

/** The routes: a template, the label used in metrics, and its handler. */
export interface Route {
  method: string;
  pattern: RegExp;
  label: string;
  handle: (context: RouteContext) => Promise<RouteAnswer>;
}

export function json(status: number, value: unknown): RouteAnswer {
  return { status, body: JSON.stringify(value), contentType: "application/json" };
}

/** The instrumented app (not listening yet): see README.md. */
export function createApp(options: AppOptions): Server {
  const histogram = new LatencyHistogram();
  const fetchFn = options.fetchFn ?? fetch;

  const routes: Route[] = [
    {
      method: "GET",
      pattern: /^\/api\/orders\/([^/]+)$/,
      label: "/api/orders/:id",
      async handle({ span, params }) {
        const id = params["0"] ?? "";
        try {
          const answer = await fetchFn(`${options.inventoryUrl}/stock/${encodeURIComponent(id)}`, {
            headers: { traceparent: formatTraceparent(span) },
          });
          if (!answer.ok) {
            await answer.body?.cancel();
            return json(502, { error: "inventory unavailable", traceId: span.traceId });
          }
          const stock = (await answer.json()) as { inStock: unknown };
          return json(200, { id, inStock: stock.inStock });
        } catch {
          return json(502, { error: "inventory unavailable", traceId: span.traceId });
        }
      },
    },
    {
      method: "GET",
      pattern: /^\/api\/boom$/,
      label: "/api/boom",
      async handle() {
        throw new Error("the order total does not add up");
      },
    },
    {
      method: "GET",
      pattern: /^\/metrics$/,
      label: "/metrics",
      async handle() {
        return { status: 200, body: histogram.render(), contentType: "text/plain; version=0.0.4" };
      },
    },
  ];

  async function handle(request: IncomingMessage, response: ServerResponse): Promise<void> {
    const started = options.now();
    const incoming = parseTraceparent(request.headers["traceparent"] as string | undefined);
    const span: SpanContext = {
      traceId: incoming?.traceId ?? newTraceId(),
      spanId: newSpanId(),
      flags: incoming?.flags ?? "01",
    };
    const method = request.method ?? "GET";
    const path = new URL(request.url ?? "/", "http://localhost").pathname;
    let label = "unmatched";
    let answer: RouteAnswer = json(404, { error: "not found" });
    let failure: string | undefined;
    for (const route of routes) {
      const match = route.method === method ? route.pattern.exec(path) : null;
      if (match) {
        label = route.label;
        const params = Object.fromEntries(match.slice(1).map((value, i) => [String(i), value]));
        try {
          answer = await route.handle({ request, span, params });
        } catch (error) {
          failure = error instanceof Error ? error.message : String(error);
          answer = json(500, { error: "internal error", traceId: span.traceId });
        }
        break;
      }
    }
    response.writeHead(answer.status, {
      "Content-Type": answer.contentType ?? "application/json",
      "X-Trace-Id": span.traceId,
    });
    response.end(answer.body);
    const durationMs = options.now() - started;
    histogram.observe({ method, route: label, status: String(answer.status) }, durationMs / 1000);
    options.log({
      level: answer.status >= 500 ? "error" : "info",
      msg: "request",
      traceId: span.traceId,
      spanId: span.spanId,
      method,
      route: label,
      status: answer.status,
      durationMs,
      ...(failure === undefined ? {} : { error: failure }),
    });
  }

  return createServer((request: IncomingMessage, response: ServerResponse) => {
    handle(request, response).catch(() => {
      response.writeHead(500);
      response.end();
    });
  });
}

if (import.meta.main) {
  createApp({
    inventoryUrl: "http://localhost:4000",
    log: (line) => console.log(JSON.stringify(line)),
    now: () => Date.now(),
  }).listen(3000, () => console.log("http://localhost:3000/api/orders/42"));
}
