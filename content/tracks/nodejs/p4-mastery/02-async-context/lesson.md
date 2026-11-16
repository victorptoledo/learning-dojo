# AsyncLocalStorage, Logging and Observability

At 3 a.m. a customer's checkout fails. Somewhere in millions of log lines are the twelve that
matter, spread over four services. Whether you find them in two minutes or two hours depends
on decisions made long before: does every line carry the request's id, are logs structured,
and can you see where the time went?

## The problem: context in async code

In a thread-per-request world, a `ThreadLocal` holds "the current request". In Node.js, one
thread interleaves thousands of requests: after an `await`, a different request may be running
when your code continues. Passing a `requestId` argument through every function works, but it
is noisy and every library in between would have to cooperate.

## AsyncLocalStorage

`AsyncLocalStorage` (`node:async_hooks`) stores a value that **follows the async flow**: any
code called from inside `run()`, including after `await`, timers, events and callbacks, sees
the same store.

```ts
import { AsyncLocalStorage } from "node:async_hooks";
import { randomUUID } from "node:crypto";

interface Context { requestId: string; userId?: string }
export const context = new AsyncLocalStorage<Context>();

createServer((req, res) => {
  const requestId = safeId(req.headers["x-request-id"]) ?? randomUUID();
  res.setHeader("x-request-id", requestId);
  context.run({ requestId }, () => handle(req, res));
});

// anywhere below, with no extra arguments:
export function currentRequestId() {
  return context.getStore()?.requestId;
}
```

- `run(store, fn)` sets the store for `fn` and everything it schedules.
- `getStore()` returns it, or `undefined` outside a `run`.
- Treat the store as **immutable** per request, or at least never share one object between
  requests.
- It is cheap in modern Node.js (built on V8's async context support), and frameworks use it
  for request scoping. A proposal (`AsyncContext`) brings the same idea to JavaScript itself.

## Structured logging

`console.log("user " + id + " paid")` is text for humans; at scale you search and aggregate
by machine. Log **one JSON object per line**:

```json
{"level":"info","time":"2026-11-15T03:02:11.120Z","msg":"payment declined","requestId":"7f9c...","orderId":"o-81","provider":"acme","ms":412}
```

pino is the usual choice for Node.js: fast (it does little on the main thread), JSON by
default, with child loggers and redaction:

```ts
import pino from "pino";

export const logger = pino({
  level: process.env.LOG_LEVEL ?? "info",
  redact: ["req.headers.authorization", "*.password", "*.cardNumber"],
  mixin: () => ({ requestId: context.getStore()?.requestId }),   // every line gets the id
});

logger.info({ orderId, ms }, "payment declined");
const paymentLog = logger.child({ component: "payments" });
```

Rules of thumb:

- **Levels**: `error` needs a human, `warn` is unusual but handled, `info` records business
  events, `debug` is off in production.
- Log **facts as fields** (`orderId`, `status`, `ms`), not inside the message string.
- Log errors as objects (`{ err }`) so the stack and `cause` are kept.
- **Never log** secrets, tokens, passwords or personal data you do not need. Redact by
  default.
- One access log line per request (method, path, status, duration, request id) is the most
  useful line you will write.

## Correlation across services

Forward the id on every outgoing call (`x-request-id`, or better, the W3C **`traceparent`**
header of distributed tracing), and accept it at the edge if it is well-formed. Now one id
finds the whole story in your log search.

## Metrics for a Node.js process

Logs tell you what happened to one request; metrics tell you how the service is doing.

| Metric | Why |
| --- | --- |
| Request rate, error rate, latency p50/p95/p99 | The user's view (the "RED" metrics) |
| **Event loop delay** (`monitorEventLoopDelay`, `performance.eventLoopUtilization()`) | The first sign of blocking or CPU saturation, specific to Node.js |
| Heap used / heap limit, GC pauses | Memory leaks and GC pressure |
| Active handles, open connections, pool usage | Resource leaks, saturation of dependencies |
| Process CPU and RSS | Capacity |

`prom-client` exposes them in Prometheus format, including default Node.js metrics.

## Tracing and diagnostics_channel

A **trace** follows one request through every service and records each step as a span with
its duration: "HTTP GET /checkout 812 ms → POST payments 430 ms → SQL INSERT 12 ms".
**OpenTelemetry** is the vendor-neutral standard. Its Node.js SDK instruments `http`, `fetch`,
popular frameworks and database drivers automatically, propagates context with
`AsyncLocalStorage`, and exports to Jaeger, Grafana Tempo, Datadog or others. Start it before
the rest of the app (`node --import ./telemetry.ts src/main.ts`).

Underneath, libraries publish events on **`diagnostics_channel`** (`node:diagnostics_channel`):
named channels such as `http.server.request.start` or `undici:request:create` that observers
subscribe to without patching code. Node.js's own `http` and `fetch` publish there, and so do
many libraries.

## Summary

- `AsyncLocalStorage.run(store, fn)` + `getStore()` carry request context through async code.
- JSON logs with levels, fields, the request id, and redaction; pino with a `mixin` reads the
  context.
- Forward ids (`x-request-id`, `traceparent`) to correlate services.
- Watch RED metrics plus event loop delay and heap.
- OpenTelemetry traces show where the time goes; `diagnostics_channel` lets tools observe
  libraries.

## Further reading

- Node.js API, "Asynchronous context tracking" (AsyncLocalStorage): <https://nodejs.org/api/async_context.html>
- Node.js API, "Diagnostics Channel": <https://nodejs.org/api/diagnostics_channel.html>
- Node.js API, `performance.eventLoopUtilization()`: <https://nodejs.org/api/perf_hooks.html#performanceeventlooputilizationutilization1-utilization2>
- OpenTelemetry, "JavaScript" (Node.js getting started): <https://opentelemetry.io/docs/languages/js/getting-started/nodejs/>
- W3C, "Trace Context": <https://www.w3.org/TR/trace-context/>
- pino documentation: <https://getpino.io/>
- prom-client: <https://github.com/siimon/prom-client>
- TC39 proposal, "AsyncContext": <https://github.com/tc39/proposal-async-context>
