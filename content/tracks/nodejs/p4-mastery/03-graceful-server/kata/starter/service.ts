// A production-ready HTTP service: request ids in every log line, safe errors, graceful shutdown.
// Useful imports: randomUUID from "node:crypto", createServer from "node:http".
import { AsyncLocalStorage } from "node:async_hooks";
import type { EventEmitter } from "node:events";
import type { IncomingMessage, Server, ServerResponse } from "node:http";

export interface RequestContext {
  requestId: string;
}

/** The context of the request being handled, available anywhere below the handler. */
export const requestContext = new AsyncLocalStorage<RequestContext>();

/** The id of the current request, or undefined outside a request. */
export function currentRequestId(): string | undefined {
  throw new Error("todo: write currentRequestId()");
}

export type Fields = Record<string, unknown>;

export interface Logger {
  info(msg: string, fields?: Fields): void;
  error(msg: string, fields?: Fields): void;
}

/**
 * A logger that writes one JSON line per call: { time, level, msg, requestId?, ...fields }.
 * requestId is added when the call happens inside a request.
 */
export function createLogger(write: (line: string) => void, now: () => Date = () => new Date()): Logger {
  throw new Error("todo: write createLogger()");
}

export type Handler = (req: IncomingMessage, res: ServerResponse) => void | Promise<void>;

export interface Service {
  server: Server;
  /** False once shutdown has started: GET /ready answers 503. */
  ready: boolean;
}

/**
 * The service, not listening yet. GET /ready is answered by the service itself; every other
 * request goes to the handler, inside requestContext, with an x-request-id header on the
 * response and one "request" log line when it finishes.
 */
export function createService(handler: Handler, logger: Logger): Service {
  throw new Error("todo: write createService()");
}

export interface ShutdownOptions {
  logger: Logger;
  /** How long in-flight requests may take before they are cut (default 10 s). */
  timeoutMs?: number;
  /** Where SIGTERM and SIGINT come from (default: process). */
  signals?: EventEmitter;
  /** How the process ends (default: process.exit). */
  exit?: (code: number) => void;
}

/**
 * Shuts the service down on SIGTERM or SIGINT: not ready any more, no new connections, idle
 * keep-alive connections closed, in-flight requests allowed to finish. Exit code 0 when they
 * all finish in time, 1 when the timeout cuts them or a second signal arrives.
 * Returns shutdown(), which starts the same sequence by hand and resolves with the exit code.
 */
export function installShutdown(service: Service, options: ShutdownOptions): { shutdown: (reason: string) => Promise<number> } {
  throw new Error("todo: write installShutdown()");
}
