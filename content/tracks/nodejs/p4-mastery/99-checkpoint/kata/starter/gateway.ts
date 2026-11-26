// A caching gateway in front of a slow product service: request context, a bounded cache,
// one upstream call per product at a time, stale answers when the upstream fails, load
// shedding, metrics and a graceful shutdown.
import { AsyncLocalStorage } from "node:async_hooks";
// Useful imports: randomUUID from "node:crypto", createServer, IncomingMessage and
// ServerResponse from "node:http".
import type { Server } from "node:http";

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
  throw new Error("todo: write currentRequestId()");
}

/** A Map that keeps at most maxEntries keys and forgets the least recently used one first. */
export class LruCache<V> {
  constructor(maxEntries: number) {
    throw new Error("todo: write LruCache");
  }

  get size(): number {
    throw new Error("todo: write LruCache.size");
  }

  /** The value, or undefined; a key that is read becomes the most recently used. */
  get(key: string): V | undefined {
    throw new Error("todo: write LruCache.get()");
  }

  /** Adds or replaces the value (the key becomes the most recently used), then evicts if needed. */
  set(key: string, value: V): void {
    throw new Error("todo: write LruCache.set()");
  }

  delete(key: string): boolean {
    throw new Error("todo: write LruCache.delete()");
  }

  /** The keys, least recently used first. */
  keys(): string[] {
    throw new Error("todo: write LruCache.keys()");
  }
}

/** The gateway (see the README), not listening yet. */
export function createGateway(options: GatewayOptions): Gateway {
  throw new Error("todo: write createGateway()");
}
