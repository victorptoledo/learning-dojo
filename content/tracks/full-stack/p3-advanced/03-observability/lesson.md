# Observability End to End: Logs, Metrics, Traces and Real User Monitoring

"It sometimes hangs" is the hardest bug report. Observability is being able to answer *what
happened to that user's request* without deploying new code. For a full stack developer the
journey starts in the browser and ends in the database, so the signals must cover all of it and
be **linked**.

## Three signals, one id

| Signal | Answers | Example |
| --- | --- | --- |
| **Logs** | what happened in this request | `{"level":"error","traceId":"4bf9...","route":"/api/checkout","status":502,"error":"payment timeout"}` |
| **Metrics** | how much and how fast, overall | p95 latency of `/api/checkout` is 2.4 s, errors at 3 % |
| **Traces** | where the time went across services | browser 3.1 s → API 2.9 s → payment provider 2.7 s |

The **trace id** is the thread through them: put it in every log line, attach it to metric
exemplars, and show it to the user in error messages ("Something went wrong. Reference
4bf92f35"). Support then finds the exact trace and log lines in seconds.

## Structured logs

- Log **JSON**, one event per line, with fixed field names (`level`, `msg`, `traceId`, `route`,
  `status`, `durationMs`), so the log platform can filter and aggregate.
- One **request log line** per request (an access log with context) plus lines for notable
  events; not a line per function call.
- **Never log secrets or personal data**: passwords, tokens, full card numbers, session cookies,
  and be careful with emails and addresses (privacy law applies to logs too).
- Use levels consistently: `error` for failures someone should look at, not for a user typing a
  wrong password.

## Metrics: RED and histograms

For every service and route, the **RED** metrics: **R**ate (requests per second), **E**rrors
(failed requests per second or as a share), **D**uration (a latency histogram). In Prometheus
naming:

```text
http_server_request_duration_seconds_bucket{method="GET",route="/api/orders/:id",status="200",le="0.25"} 941
```

- Latency is a **histogram**, so you can compute percentiles (p50, p95, p99) across instances;
  averages hide the slow tail that users feel.
- Labels must have **low cardinality**: the route template `/api/orders/:id`, never the raw path
  `/api/orders/48213`, never a user id. Every new label value is a new time series.
- Add **business metrics** too: orders per minute, failed payments. They often notice a broken
  release first.

## Distributed traces

A trace is a tree of **spans**, one per unit of work (an HTTP request, a query, a call to another
service). The **W3C Trace Context** header carries it between processes:

```http
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
             version-trace id (32 hex)-parent span id (16 hex)-flags (01 = sampled)
```

Each service keeps the trace id, creates its own span id, and sends `traceparent` with its span
id as the parent on outgoing calls. **OpenTelemetry** does this automatically for common
frameworks (Node.js, Spring Boot through Micrometer Tracing, .NET, Go, Python) and exports
traces, metrics and logs to any backend (Jaeger, Tempo, Honeycomb, Datadog...). Sampling keeps
costs down; keep all error traces if your backend supports tail sampling.

The browser can start the trace too (OpenTelemetry for the web), so a slow page load shows the
time spent before the API was even called. Remember that the API's CORS configuration must allow
the `traceparent` header.

## The front end

Server metrics say nothing about a JavaScript error that breaks a button. Add:

- **error reporting** (Sentry, OpenTelemetry, your own `window.onerror` and
  `unhandledrejection` handlers posting to an endpoint), with source maps uploaded privately
  so stack traces are readable;
- **real-user monitoring** (RUM) of Core Web Vitals with the `web-vitals` library, sent with
  `navigator.sendBeacon`, broken down by page and device;
- the trace id of failed API calls in the error report.

## Alerts and dashboards

Alert on **symptoms users feel**, against SLOs (the Backend Developer track covers SLIs and
error budgets): checkout error rate above 1 % for 10 minutes, p95 latency of the home page above
1 s. Do not page anyone because CPU is at 80 % if users are fine. A good first dashboard per
service has the RED metrics per route, dependency latency and the deploy markers.

## Summary

- Logs, metrics and traces answer different questions; the trace id links them and reaches
  the user's error message.
- Structured JSON logs, one per request, no secrets.
- RED metrics with histograms and route-template labels.
- W3C `traceparent` carries traces across services; OpenTelemetry does the plumbing.
- Watch the front end too (errors, Web Vitals) and alert on symptoms.

## Further reading

- W3C, "Trace Context": <https://www.w3.org/TR/trace-context/>
- OpenTelemetry documentation, "Observability primer": <https://opentelemetry.io/docs/concepts/observability-primer/>
- Prometheus documentation, "Histograms and summaries": <https://prometheus.io/docs/practices/histograms/>
- Prometheus documentation, "Metric and label naming": <https://prometheus.io/docs/practices/naming/>
- Google SRE book, "Monitoring Distributed Systems": <https://sre.google/sre-book/monitoring-distributed-systems/>
- OWASP, "Logging Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html>
- In the dojo: the DevOps & Cloud track's *Observability* and *SLIs, SLOs, Error Budgets and Burn-Rate Alerting*, the Node.js track's *AsyncLocalStorage, Logging and Observability* and the Frontend track's *Frontend in Production*.
