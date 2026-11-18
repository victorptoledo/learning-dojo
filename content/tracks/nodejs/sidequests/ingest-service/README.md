# Sidequest: Event ingestion service

Analytics, logs and IoT readings all start the same way: clients send a stream of small JSON
events, and a service stores them in batches. It looks simple until production happens: a
client uploads 2 GB at once, the disk is slow, the disk fails, the orchestrator sends
`SIGTERM` in the middle of an upload, and someone asks "how slow is it right now?". Build that
service on `node:http`, and make it behave well in every one of those moments.

```text
$ curl -X POST localhost:3000/events -H "content-type: application/x-ndjson" --data-binary @events.ndjson
{"accepted":99998,"rejected":2,"errors":[{"line":17,"message":"not valid JSON"},{"line":901,"message":"time must be a date and time"}]}
$ curl localhost:3000/metrics
# HELP ingest_events_total Events received, by result.
# TYPE ingest_events_total counter
ingest_events_total{result="accepted"} 99998
...
```

`service.ts` holds the service: `createService(options)` returns the `node:http` server and
a few controls, and batches go to a `Sink` (`fileSink(dir)` writes files; the tests use one
they can slow down, stop or make fail). `main.ts` starts it (`npm start`). The contract tests
start services on random ports, upload with raw `node:http` requests whose chunks and pauses
they choose, read `/metrics`, and in the last stage run `main.ts` as a real process and send it
signals. Routes, JSON shapes and metric names are fixed; the rest is yours. Only Node.js
itself. `tests/` is yours too.
