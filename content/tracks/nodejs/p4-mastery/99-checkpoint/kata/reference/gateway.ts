// A caching gateway in front of a slow product service: request context, a bounded cache,
// one upstream call per product at a time, stale answers when the upstream fails, load
// shedding, metrics and a graceful shutdown.
import { AsyncLocalStorage } from "node:async_hooks";
import { randomUUID } from "node:crypto";
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";

export interface GatewayOptions {
  /** The product service, for example "http://127.0.0.1:4000"; products are at /products/<id>. */
  upstream: string;
  /** How long a cached product is fresh. */
  ttlMs?: number;
  /** How many products the cache keeps at most. */
  maxEntries?: number;
  /** How many /products requests may be in progress before new ones get 503. */
  maxInFlight?: number;
  /** How long to wait for the upstream. */
  timeoutMs?: number;
  /** Receives one JSON log line at a time (without the newline). */
  log?: (line: string) => void;
  /** The clock for the cache (milliseconds); the tests move it by hand. */
  now?: () => number;
}

export interface Metrics {
  requests: number;
  hits: number;
  misses: number;
  stale: number;
  shed: number;
  upstreamCalls: number;
  inFlight: number;
  cacheEntries: number;
}

export interface Gateway {
  server: Server;
  /** false once shutdown has started. */
  readonly ready: boolean;
  metrics(): Metrics;
  /** Stops accepting connections, lets requests in progress finish, forces the rest after graceMs. */
  shutdown(graceMs?: number): Promise<void>;
}

export const DEFAULTS = { ttlMs: 30_000, maxEntries: 1_000, maxInFlight: 100, timeoutMs: 1_000 } as const;

export const requestContext = new AsyncLocalStorage<{ requestId: string }>();

export const REQUEST_ID = /^[A-Za-z0-9_-]{1,64}$/;
export const PRODUCT_ID = /^[a-z0-9-]{1,40}$/;

/** The id of the request being handled, or undefined outside a request. */
export function currentRequestId(): string | undefined {
  return requestContext.getStore()?.requestId;
}

/** A Map that keeps at most maxEntries keys and forgets the least recently used one first. */
export class LruCache<V> {
  readonly #max: number;
  readonly #map = new Map<string, V>();

  constructor(maxEntries: number) {
    if (!Number.isInteger(maxEntries) || maxEntries < 1) {
      throw new RangeError(`maxEntries must be a whole number of at least 1, got ${maxEntries}`);
    }
    this.#max = maxEntries;
  }

  get size(): number {
    return this.#map.size;
  }

  get(key: string): V | undefined {
    const value = this.#map.get(key);
    if (value !== undefined) {
      this.#map.delete(key); // a Map keeps insertion order: move the key to the end
      this.#map.set(key, value);
    }
    return value;
  }

  set(key: string, value: V): void {
    this.#map.delete(key);
    this.#map.set(key, value);
    if (this.#map.size > this.#max) {
      const oldest = this.#map.keys().next().value as string;
      this.#map.delete(oldest);
    }
  }

  delete(key: string): boolean {
    return this.#map.delete(key);
  }

  /** The keys, least recently used first. */
  keys(): string[] {
    return [...this.#map.keys()];
  }
}

interface Entry {
  body: string;
  storedAt: number;
}

type Outcome = { kind: "found"; body: string } | { kind: "not-found" } | { kind: "timeout" } | { kind: "failed"; reason: string };

function sendJson(res: ServerResponse, status: number, body: unknown, headers: Record<string, string> = {}): void {
  const text = typeof body === "string" ? body : JSON.stringify(body);
  res.writeHead(status, { "content-type": "application/json", "content-length": Buffer.byteLength(text), ...headers });
  res.end(text);
}

export function createGateway(options: GatewayOptions): Gateway {
  const ttlMs = options.ttlMs ?? DEFAULTS.ttlMs;
  const maxInFlight = options.maxInFlight ?? DEFAULTS.maxInFlight;
  const timeoutMs = options.timeoutMs ?? DEFAULTS.timeoutMs;
  const now = options.now ?? Date.now;
  const write = options.log ?? ((line: string) => process.stdout.write(`${line}\n`));
  const cache = new LruCache<Entry>(options.maxEntries ?? DEFAULTS.maxEntries);
  const pending = new Map<string, Promise<Outcome>>();
  const counts = { requests: 0, hits: 0, misses: 0, stale: 0, shed: 0, upstreamCalls: 0 };
  let inFlight = 0;
  let ready = true;
  let stopping: Promise<void> | undefined;
  /** Where each answer came from, for the access log. */
  const served = new WeakMap<ServerResponse, "hit" | "miss" | "stale">();

  function log(level: "info" | "error", msg: string, fields: Record<string, unknown> = {}): void {
    const requestId = currentRequestId();
    write(JSON.stringify({ time: new Date().toISOString(), level, msg, ...(requestId === undefined ? {} : { requestId }), ...fields }));
  }

  async function callUpstream(id: string): Promise<Outcome> {
    counts.upstreamCalls += 1;
    try {
      const response = await fetch(new URL(`/products/${id}`, options.upstream), {
        headers: { "x-request-id": currentRequestId() ?? randomUUID(), accept: "application/json" },
        signal: AbortSignal.timeout(timeoutMs),
      });
      const text = await response.text();
      if (response.status === 404) {
        return { kind: "not-found" };
      }
      if (response.status !== 200) {
        return { kind: "failed", reason: `upstream answered ${response.status}` };
      }
      JSON.parse(text); // never cache or forward a body that is not JSON
      return { kind: "found", body: text };
    } catch (error) {
      if (error instanceof Error && error.name === "TimeoutError") {
        return { kind: "timeout" };
      }
      return { kind: "failed", reason: error instanceof Error ? error.message : String(error) };
    }
  }

  /** One upstream call per product at a time: concurrent misses share it. */
  function load(id: string): Promise<Outcome> {
    let promise = pending.get(id);
    if (promise === undefined) {
      promise = callUpstream(id).finally(() => pending.delete(id));
      pending.set(id, promise);
    }
    return promise;
  }

  async function product(id: string, res: ServerResponse): Promise<void> {
    const entry = cache.get(id);
    if (entry !== undefined && now() - entry.storedAt < ttlMs) {
      counts.hits += 1;
      served.set(res, "hit");
      sendJson(res, 200, entry.body, { "x-cache": "hit" });
      return;
    }
    counts.misses += 1;
    const outcome = await load(id);
    if (outcome.kind === "found") {
      cache.set(id, { body: outcome.body, storedAt: now() });
      served.set(res, "miss");
      sendJson(res, 200, outcome.body, { "x-cache": "miss" });
      return;
    }
    if (outcome.kind === "not-found") {
      cache.delete(id);
      sendJson(res, 404, { error: "not found" });
      return;
    }
    log("error", "upstream failed", { product: id, error: outcome.kind === "failed" ? outcome.reason : "timeout" });
    const stale = cache.get(id);
    if (stale !== undefined) {
      counts.stale += 1;
      served.set(res, "stale");
      sendJson(res, 200, stale.body, { "x-cache": "stale" });
      return;
    }
    const requestId = currentRequestId();
    if (outcome.kind === "timeout") {
      sendJson(res, 504, { error: "upstream timeout", requestId });
    } else {
      sendJson(res, 502, { error: "upstream unavailable", requestId });
    }
  }

  async function route(req: IncomingMessage, res: ServerResponse): Promise<void> {
    const { pathname } = new URL(req.url ?? "/", "http://localhost");
    if (pathname === "/ready") {
      sendJson(res, ready ? 200 : 503, { ready });
      return;
    }
    if (pathname === "/metrics") {
      sendJson(res, 200, metrics());
      return;
    }
    const match = /^\/products\/([^/]+)$/.exec(pathname);
    if (match === null) {
      sendJson(res, 404, { error: "not found" });
      return;
    }
    if (req.method !== "GET") {
      sendJson(res, 405, { error: "method not allowed" }, { allow: "GET" });
      return;
    }
    if (inFlight >= maxInFlight) {
      counts.shed += 1;
      sendJson(res, 503, { error: "overloaded" }, { "retry-after": "1" });
      return;
    }
    inFlight += 1;
    res.once("close", () => {
      inFlight -= 1;
    });
    let id: string;
    try {
      id = decodeURIComponent(match[1]!);
    } catch {
      id = ""; // a malformed escape such as %E0%A4%A
    }
    if (!PRODUCT_ID.test(id)) {
      sendJson(res, 400, { error: "invalid product id" });
      return;
    }
    await product(id, res);
  }

  function metrics(): Metrics {
    return { ...counts, inFlight, cacheEntries: cache.size };
  }

  const server = createServer((req, res) => {
    const header = req.headers["x-request-id"];
    const requestId = typeof header === "string" && REQUEST_ID.test(header) ? header : randomUUID();
    res.setHeader("x-request-id", requestId);
    counts.requests += 1;
    const started = performance.now();
    requestContext.run({ requestId }, () => {
      res.once("finish", () => {
        const cache = served.get(res);
        log("info", "request", { method: req.method, path: req.url, status: res.statusCode, ...(cache === undefined ? {} : { cache }), ms: Math.round(performance.now() - started) });
      });
      route(req, res).catch((error: unknown) => {
        log("error", "request failed", { error: error instanceof Error ? error.message : String(error) });
        if (!res.headersSent) {
          sendJson(res, 500, { error: "internal error", requestId });
        } else {
          res.destroy();
        }
      });
    });
  });

  function shutdown(graceMs = 10_000): Promise<void> {
    if (stopping !== undefined) {
      return stopping;
    }
    ready = false;
    log("info", "shutting down", { graceMs });
    stopping = new Promise<void>((resolve) => {
      const timer = setTimeout(() => server.closeAllConnections(), graceMs);
      server.close(() => {
        clearTimeout(timer);
        resolve();
      });
      server.closeIdleConnections();
    });
    return stopping;
  }

  return {
    server,
    get ready() {
      return ready;
    },
    metrics,
    shutdown,
  };
}
