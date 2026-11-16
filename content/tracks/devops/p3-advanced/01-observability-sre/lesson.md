# Observability: Metrics, Logs and Distributed Tracing

Monitoring answers questions you thought of in advance ("is the disk full?"). **Observability**
is the ability to answer questions you did not anticipate ("why are checkouts from one region
slow since Tuesday?") from the telemetry a system emits. It rests on three signals, each good
at different things.

| Signal | What it is | Good for | Cost driver |
| --- | --- | --- | --- |
| **Metrics** | numbers aggregated over time, with labels | dashboards, alerts, trends, cheap long retention | the number of distinct label combinations (cardinality) |
| **Logs** | timestamped events, ideally structured | the detail of one event: an error, a decision, an audit trail | volume |
| **Traces** | the path of one request through services, as timed spans | where time goes across services, which dependency fails | volume, controlled by sampling |

## Metrics with Prometheus

**Prometheus** scrapes `/metrics` endpoints every 15–60 s and stores time series identified by
a name and labels: `http_requests_total{service="checkout", route="/pay", code="500"}`.
The metric types:

- **counter**: only goes up (requests, errors, bytes); always read it through `rate()`;
- **gauge**: goes up and down (queue length, memory in use, temperature);
- **histogram**: counts observations in buckets (`_bucket{le="0.25"}`), plus `_sum` and
  `_count`; the basis for latency percentiles that can be aggregated across instances.

The PromQL you will use every day:

```promql
# requests per second, per route, over the last 5 minutes
sum by (route) (rate(http_requests_total{service="checkout"}[5m]))

# error ratio
sum(rate(http_requests_total{service="checkout", code=~"5.."}[5m]))
  / sum(rate(http_requests_total{service="checkout"}[5m]))

# p99 latency from a histogram
histogram_quantile(0.99,
  sum by (le) (rate(http_request_duration_seconds_bucket{service="checkout"}[5m])))
```

Two habits keep metrics useful:

- **Use percentiles, not averages.** An average of 120 ms can hide 2% of requests taking 4 s;
  p99 shows them.
- **Keep cardinality bounded.** Every distinct label set is a new series. `route="/orders/{id}"`
  is fine; `user_id`, `order_id`, full URLs or error messages as labels will take your
  Prometheus down. Put those in logs or traces.

Two frameworks tell you what to measure: **RED** for services (Rate, Errors, Duration) and
**USE** for resources (Utilisation, Saturation, Errors). Google's **four golden signals** are
latency, traffic, errors and saturation. **Recording rules** precompute expensive queries,
and long-term storage (Thanos, Mimir, a managed service) keeps months of data.

## Logs

Write **structured** logs (JSON or logfmt), one event per line, to stdout; let the platform
ship them (Fluent Bit, Vector, the OpenTelemetry Collector) to a store (Loki, Elasticsearch or
OpenSearch, a cloud service).

```json
{"ts":"2026-11-15T10:02:11Z","level":"error","service":"checkout","trace_id":"4bf92f3577b34da6a3ce929d0e0e4736","msg":"payment declined","provider":"acme-pay","status":402,"duration_ms":812}
```

Rules: a consistent schema (`level`, `service`, `trace_id`, `msg`), no secrets or personal
data, levels that mean something (error is "someone should look"), and sampling or rate limits
for noisy debug logs. Logs are the most expensive signal per byte; do not use them as metrics.

## Distributed tracing

A **trace** is a tree of **spans**: each span is one unit of work (an HTTP handler, a database
query, a call to another service) with a start, a duration, attributes and a status. The
**trace context** (W3C `traceparent` header: trace ID and parent span ID) is propagated on
every call, so spans from different services join into one trace.

```text
checkout POST /pay ─────────────────────────────────── 1.24 s
  ├─ cart GET /cart/42 ───── 40 ms
  ├─ db SELECT orders ──── 12 ms
  └─ acme-pay POST /charge ───────────────────── 1.15 s   ◄─ here
```

**OpenTelemetry** (OTel) is the vendor-neutral standard: SDKs and auto-instrumentation for
every major language, a common data model for traces, metrics and logs, and the **Collector**,
which receives, processes (batching, sampling, removing attributes) and exports to any backend
(Jaeger, Tempo, Zipkin, a vendor). Tracing every request is expensive, so traces are
**sampled**: head sampling (decide at the start, for example 10%) is cheap; tail sampling (in
the Collector, after the trace ends) can keep all errors and slow traces.

## Correlation

The signals are most useful together:

1. a **dashboard** shows p99 latency rising for `/pay` at 10:00;
2. **exemplars** on the histogram, or a trace search for slow `/pay` requests, give trace IDs;
3. the **trace** shows the time is spent in the payment provider call;
4. the **logs** with that `trace_id` show the provider's timeouts and retries.

Put the trace ID in every log line, use the same `service` and `environment` names in every
signal (OTel resource attributes), and link dashboards to trace and log queries.

## Dashboards

A good dashboard answers one question for one audience: "is checkout healthy?" for on-call,
"how is capacity trending?" for planning. Put the user-facing signals (traffic, errors,
latency percentiles, saturation) at the top, then the dependencies, then the internals. Use
the same time range everywhere and avoid walls of graphs nobody reads.

## Summary

- Metrics for trends and alerts, logs for event detail, traces for where time goes across
  services.
- Prometheus counters through `rate()`, histograms for percentiles; keep label cardinality
  bounded.
- Structured logs with a trace ID, shipped from stdout, without secrets.
- OpenTelemetry propagates trace context and exports all signals; sample traces wisely.
- Correlate: dashboard → exemplar or trace → span → logs.

## Further reading

- Prometheus querying basics: <https://prometheus.io/docs/prometheus/latest/querying/basics/>
- Prometheus histograms and summaries: <https://prometheus.io/docs/practices/histograms/>
- OpenTelemetry documentation: <https://opentelemetry.io/docs/>
- W3C Trace Context: <https://www.w3.org/TR/trace-context/>
- *Observability Engineering*, Charity Majors, Liz Fong-Jones and George Miranda (O'Reilly, 2022)
