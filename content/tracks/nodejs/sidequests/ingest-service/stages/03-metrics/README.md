"Is it slow? Is it losing data? Is it keeping up?" Answer with numbers. `GET /metrics`
returns them in the Prometheus text format (content type
`text/plain; version=0.0.4; charset=utf-8`), so any Prometheus can scrape the service.

Every family has a `# HELP <name> <text>` and a `# TYPE <name> <type>` line, also before any
traffic, and these samples:

| Family | Type | Samples |
| --- | --- | --- |
| `ingest_events_total` | counter | `{result="accepted"}` and `{result="rejected"}` |
| `ingest_requests_total` | counter | one per route and status seen: `{route="/events",status="202"}`; `route` is `/events`, `/metrics` (the scrape is counted after it answers, so the next scrape shows it), later the health routes, and `other` for any other path |
| `ingest_request_duration_seconds` | histogram | the time from the start of each `POST /events` request to its answer: `_bucket{le="..."}` for 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10 and `+Inf`, then `_sum` and `_count` |
| `ingest_batches_written_total` | counter | one sample, no labels |
| `ingest_pending_batches` | gauge | `stats().pendingBatches` |
| `ingest_buffered_events` | gauge | `stats().bufferedEvents` |
| `ingest_event_loop_delay_max_seconds` | gauge | the longest event loop delay since the previous scrape, in seconds |

- Histogram buckets are **cumulative**: `le="0.25"` counts the requests that took at most
  0.25 s, so each bucket is at least the one before, and `+Inf` equals `_count`.
- For the event loop delay, `monitorEventLoopDelay` from `node:perf_hooks` keeps a histogram
  of how late its timer fires: read its `max` (nanoseconds) and `reset()` it on each scrape.
  A synchronous 250 ms block anywhere in the process must show up as at least 0.15.
- Measure durations with `process.hrtime.bigint()` or `performance.now()`, not `Date`: they do
  not jump when the clock is adjusted.
