An orchestrator (Kubernetes, a load balancer) asks two questions: is the process alive, and
should it get traffic now? And disks fail: a failed write must not lose a batch.

## Health checks

- `GET /health/live` answers `200 { "status": "ok" }` whenever the process can answer at all.
- `GET /health/ready` answers `200 { "status": "ready" }`, or `503` with
  `{ "status": "not ready", "reasons": [...] }` listing why:
  `"backpressure"` while `maxPending` batches are pending, and `"sink failing"` from a failed
  write until a write works again.

Both routes count in `ingest_requests_total` with their own `route`.

## A sink that fails

`ServiceOptions` gains `retryMs` (default 100).

- When `sink.write` rejects, write **the same batch** again after `retryMs`, then after twice
  that, four times, and so on (it is wise to cap the wait, for instance at 5 s), until it
  works. Nothing is lost and nothing is written twice.
- Batches stay in order and still go one at a time: the next batch waits while one is being
  retried, which means backpressure and load shedding start as soon as the failures last.
- Count every failed write in a new counter, `ingest_sink_failures_total`.
