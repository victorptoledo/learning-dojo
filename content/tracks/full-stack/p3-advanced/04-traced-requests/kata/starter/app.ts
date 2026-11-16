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
  void histogram;
  void formatTraceparent;
  void newSpanId;
  void newTraceId;
  void parseTraceparent;
  return createServer((request: IncomingMessage, response: ServerResponse) => {
    // todo: the routes of README.md, wrapped so that every request gets a trace, an
    // X-Trace-Id header, a log line and a histogram observation.
    void options;
    response.writeHead(501, { "Content-Type": "application/json" });
    response.end(JSON.stringify({ error: `todo: ${request.method} ${request.url} is not written yet` }));
  });
}

if (import.meta.main) {
  createApp({
    inventoryUrl: "http://localhost:4000",
    log: (line) => console.log(JSON.stringify(line)),
    now: () => Date.now(),
  }).listen(3000, () => console.log("http://localhost:3000/api/orders/42"));
}
