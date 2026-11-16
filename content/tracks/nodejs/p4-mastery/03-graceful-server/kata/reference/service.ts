// A production-ready HTTP service: request ids in every log line, safe errors, graceful shutdown.
import { AsyncLocalStorage } from "node:async_hooks";
import { randomUUID } from "node:crypto";
import type { EventEmitter } from "node:events";
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";

export interface RequestContext {
  requestId: string;
}

/** The context of the request being handled, available anywhere below the handler. */
export const requestContext = new AsyncLocalStorage<RequestContext>();

/** The id of the current request, or undefined outside a request. */
export function currentRequestId(): string | undefined {
  return requestContext.getStore()?.requestId;
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
  const log = (level: "info" | "error", msg: string, fields: Fields = {}) => {
    const requestId = currentRequestId();
    write(JSON.stringify({ time: now().toISOString(), level, msg, ...(requestId === undefined ? {} : { requestId }), ...fields }));
  };
  return {
    info: (msg, fields) => log("info", msg, fields),
    error: (msg, fields) => log("error", msg, fields),
  };
}

export type Handler = (req: IncomingMessage, res: ServerResponse) => void | Promise<void>;

export interface Service {
  server: Server;
  /** False once shutdown has started: GET /ready answers 503. */
  ready: boolean;
}

const REQUEST_ID = /^[A-Za-z0-9_-]{1,64}$/;

function sendJson(res: ServerResponse, status: number, body: unknown): void {
  const text = JSON.stringify(body);
  res.writeHead(status, { "content-type": "application/json; charset=utf-8", "content-length": Buffer.byteLength(text) });
  res.end(text);
}

/**
 * The service, not listening yet. GET /ready is answered by the service itself; every other
 * request goes to the handler, inside requestContext, with an x-request-id header on the
 * response and one "request" log line when it finishes.
 */
export function createService(handler: Handler, logger: Logger): Service {
  const service: Service = { server: createServer(), ready: true };

  service.server.on("request", (req: IncomingMessage, res: ServerResponse) => {
    const incoming = req.headers["x-request-id"];
    const requestId = typeof incoming === "string" && REQUEST_ID.test(incoming) ? incoming : randomUUID();
    const started = performance.now();
    res.setHeader("x-request-id", requestId);
    if (!service.ready) {
      res.setHeader("connection", "close"); // do not keep connections alive while shutting down
    }

    requestContext.run({ requestId }, () => {
      res.on("finish", () => {
        logger.info("request", {
          method: req.method,
          path: req.url,
          status: res.statusCode,
          ms: Math.round(performance.now() - started),
        });
      });

      if (req.url === "/ready") {
        sendJson(res, service.ready ? 200 : 503, { ready: service.ready });
        return;
      }

      Promise.resolve()
        .then(() => handler(req, res))
        .catch((error: unknown) => {
          logger.error("request failed", { error: error instanceof Error ? error.message : String(error) });
          if (res.headersSent) {
            res.destroy();
          } else {
            sendJson(res, 500, { error: "internal error", requestId });
          }
        });
    });
  });

  return service;
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
  const { logger, timeoutMs = 10_000, signals = process, exit = (code: number) => process.exit(code) } = options;
  let finished: Promise<number> | undefined;
  let forceNow: (() => void) | undefined;

  const shutdown = (reason: string): Promise<number> => {
    if (finished !== undefined) {
      return finished;
    }
    service.ready = false;
    logger.info("shutting down", { reason });
    finished = new Promise<number>((resolve) => {
      let done = false;
      const finish = (code: number) => {
        if (done) {
          return;
        }
        done = true;
        clearTimeout(timer);
        logger.info(code === 0 ? "shutdown complete" : "shutdown forced", { code });
        exit(code);
        resolve(code);
      };
      const timer = setTimeout(() => {
        service.server.closeAllConnections();
        finish(1);
      }, timeoutMs);
      service.server.close(() => finish(0));
      service.server.closeIdleConnections();
      forceNow = () => {
        service.server.closeAllConnections();
        finish(1);
      };
    });
    return finished;
  };

  const onSignal = (signal: string) => {
    if (finished === undefined) {
      void shutdown(signal);
    } else {
      logger.info("second signal: exiting now", { signal });
      forceNow?.();
    }
  };
  signals.on("SIGTERM", () => onSignal("SIGTERM"));
  signals.on("SIGINT", () => onSignal("SIGINT"));
  return { shutdown };
}
