# Traced Requests: Logs, Metrics and Traces That Line Up

A customer reports "the order page failed at 10:42". Today the browser's error, the API's logs
and the inventory service's logs cannot be joined: nothing ties them together. Make the orders
API observable the way OpenTelemetry-instrumented services are, by hand, so you see every
moving part:

- every request belongs to a **trace**, taken from the W3C `traceparent` header the browser (or
  the previous service) sent, or started here;
- each request writes **one structured JSON log line** with the trace id;
- calls to the inventory service **propagate** the trace;
- `GET /metrics` exposes a **latency histogram** in the Prometheus text format, labelled by
  route template (never by raw path).

## `trace.ts`

A `traceparent` is `VERSION-TRACEID-PARENTID-FLAGS`: `00`, 32 lower-case hex digits, 16
lower-case hex digits and 2 hex digits, for example
`00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01`.

| Function | Does |
| --- | --- |
| `parseTraceparent(header)` | `{ traceId, parentId, flags }`, or `undefined` when the header is missing or invalid: wrong shape or length, upper-case hex, version `ff`, or a trace id or parent id made only of zeros |
| `formatTraceparent({ traceId, spanId, flags })` | The header for an outgoing call: `00-TRACEID-SPANID-FLAGS` |

`newTraceId()` and `newSpanId()` are written for you.

## `metrics.ts`

`LatencyHistogram` records request durations in seconds with the buckets of `BUCKETS`:

- `observe(labels, seconds)` with labels `{ method, route, status }`;
- `render()` returns the Prometheus text format, one series per label set, in the order the label
  sets were first seen:

```text
# HELP http_server_request_duration_seconds Duration of HTTP server requests.
# TYPE http_server_request_duration_seconds histogram
http_server_request_duration_seconds_bucket{method="GET",route="/api/orders/:id",status="200",le="0.005"} 0
...
http_server_request_duration_seconds_bucket{method="GET",route="/api/orders/:id",status="200",le="+Inf"} 3
http_server_request_duration_seconds_sum{method="GET",route="/api/orders/:id",status="200"} 0.42
http_server_request_duration_seconds_count{method="GET",route="/api/orders/:id",status="200"} 3
```

Buckets are **cumulative** (`le` = "less than or equal"), every bucket of `BUCKETS` is printed
(as `String(bound)`) and then `+Inf`; the sum is printed with `String(sum)`.

## `app.ts`

`createApp(options)` gets `inventoryUrl`, `log` (called with one object per log line), `now`
(milliseconds, for durations), and optionally `fetchFn`. For **every** request:

1. Take the trace from `traceparent` when it is valid (keep its trace id and flags), otherwise
   start a new trace (`newTraceId()`, flags `01`). The server's own span gets `newSpanId()`.
2. Answer with the header `X-Trace-Id: <trace id>`.
3. When the answer is sent, log `{ level, msg: "request", traceId, spanId, method, route, status,
   durationMs }`: `level` is `"error"` for a 5xx and `"info"` otherwise; `durationMs` comes from
   `now()`.
4. Record the duration (in seconds) in the histogram.

| Route | Answer |
| --- | --- |
| `GET /api/orders/:id` (route label `/api/orders/:id`) | Calls `GET {inventoryUrl}/stock/:id` with `traceparent: 00-<same trace id>-<this request's span id>-<flags>`. `200` `{"id": "...", "inStock": <the inventory's inStock>}`. When the inventory answers non-2xx or cannot be reached: `502` `{"error": "inventory unavailable", "traceId": "..."}` |
| `GET /api/boom` (route `/api/boom`) | Its handler throws: `500` `{"error": "internal error", "traceId": "..."}`, and the log line also has `error` with the exception message |
| `GET /metrics` (route `/metrics`) | `200`, `Content-Type: text/plain; version=0.0.4`, the histogram |
| anything else (route `unmatched`) | `404` `{"error": "not found"}` |

The trace id in error bodies is what support asks the customer for.

## How the kata is checked

`tsc` checks every file, then `node:test` starts your app and a fake inventory service on
`127.0.0.1`, captures the log lines and the headers the inventory received, and reads `/metrics`.
There are 14 tests: 2 in `tests/visible.test.ts` and 12 hidden ones. The functions that still
throw `new Error("todo ...")` are yours to write.
