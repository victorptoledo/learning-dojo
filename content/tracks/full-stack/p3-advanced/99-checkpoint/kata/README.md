# Checkpoint 3: Live Order Tracker

The order-tracking page polls the API every two seconds, and the API answers its list with one
query per order. The team wants live updates and is about to run **two instances** behind a
load balancer with rolling deploys. Today the API only lists orders (with an N+1) and answers
the liveness probe. Make it ready for production:

- the list page costs **two queries**, whatever its size;
- status changes are **pushed** over Server-Sent Events, reach the clients of **every
  instance** exactly once, and a client that reconnects **resumes** where it stopped;
- streams **clean up** after themselves and show up in `/metrics`;
- the instance can be **drained** for a deploy without hanging on open streams.

`store.ts` (the shared database, which counts queries) and `bus.ts` (the pub/sub every instance
shares, like Redis) are written for you. You write `events.ts` and finish `app.ts`.

## `events.ts`

| Function | Does |
| --- | --- |
| `formatEvent({ id?, event?, data })` | `id: <id>\n` when there is an id, `event: <event>\n` when there is an event, then one `data: <line>\n` for each line of `data` (split on `\n`, `\r\n` or `\r`), then a blank line |
| `formatComment(text)` | `: <text>\n\n` (a comment, used as a heartbeat) |
| `parseLastEventId(header)` | The number when the header is a whole number made of digits only (`"0"`, `"7"`), otherwise `undefined` |

## `app.ts`

`createApp({ store, bus, heartbeatMs = 15000, retryMs = 3000, drainDelayMs = 5000 })` returns
`{ server, drain }`. Errors are JSON `{ "error": "..." }`.

| Route | Answer |
| --- | --- |
| `GET /api/orders?ids=3,1,2` | `200` `{ "orders": [{ id, customer, status, version, items }] }` in the order of `ids`, unknown ids left out, with **at most two queries** (`getMany`, `itemsOfMany`). `400` when `ids` is missing or empty, has more than 50 ids, or any id is not a positive whole number |
| `POST /api/orders/:id/status` with `{ "status": "packed" }` | `404` unknown order; `422` when `status` is not one of `STATUSES`; `409` `{ error, current }` unless the new status comes **after** the current one in `STATUSES`; otherwise save it with `store.setStatus`, publish the change on the bus channel `order:<id>` (the change as JSON) and answer `200` with the change `{ orderId, status, version }` |
| `GET /api/orders/:id/events` | `404` unknown order. Otherwise `200` with `Content-Type: text/event-stream` and `Cache-Control: no-cache`, then the frames below |
| `GET /healthz` | `200` `{ "status": "ok" }`, even while draining (the process is alive) |
| `GET /readyz` | `200` `{ "status": "ready" }`; `503` `{ "status": "draining" }` once `drain()` was called |
| `GET /metrics` | `200` `text/plain; version=0.0.4` with the gauge `sse_clients_open` (streams open now) and the counter `sse_events_sent_total` (status events written by this instance), each with `# HELP` and `# TYPE` lines |
| anything else | `404` |

### The event stream

1. The first frame is `retry: <retryMs>\n\n`.
2. Without a valid `Last-Event-ID`, send the order's **current state** as one status event.
   With `Last-Event-ID: n`, send instead every change after version `n`
   (`store.historyAfter`), oldest first.
3. Then send every change the **bus** delivers for this order. A status event is
   `formatEvent({ id: version, event: "status", data: JSON.stringify({ orderId, status, version }) })`.
   A client must never get the same version twice, nor an older one after a newer one.
4. Every `heartbeatMs`, write `formatComment("ping")`.
5. When the client goes away, stop the heartbeat, **unsubscribe** from the bus and stop counting
   the stream as open.

The instance that saves a change hears about it from the bus like every other instance: if it
also wrote to its own clients directly, they would get the change twice.

### Draining

`drain()` is what the process does on `SIGTERM` during a rolling deploy:

1. At once, `/readyz` answers `503`, and new event streams get `503` with `Retry-After: 1`
   (other requests are still served), so the load balancer moves traffic away.
2. After `drainDelayMs`, end every open stream (browsers' `EventSource` reconnects, to another
   instance) and close the server. The promise resolves when the server has closed.

`server.close()` waits for every open connection, and an event stream never finishes on its own.

## How the kata is checked

`tsc` checks every file, then `node:test` starts one or two instances sharing a store and a bus
on `127.0.0.1`, reads the streams with plain HTTP clients and counts the store's queries. There
are 16 tests: 2 in `tests/visible.test.ts` and 14 hidden ones. Run `npm install` once and
`npm test` to run the visible ones.
