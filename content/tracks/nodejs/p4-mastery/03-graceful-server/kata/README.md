# Graceful Server

In production a service is stopped all the time: every deploy, every scale-down, every node
drain sends it `SIGTERM`. A service that just dies cuts requests in half. One that never
stops gets `SIGKILL` from the orchestrator anyway. And when something goes wrong, you need
to find all the log lines of **one** request among thousands. Write the pieces that solve
this, in `service.ts`. `main.ts` runs them with `npm start`.

## 1. Request context and logging

`requestContext` (an `AsyncLocalStorage`) is given.

| Function | Does |
| --- | --- |
| `currentRequestId()` | The `requestId` of the current context, or `undefined` outside a request |
| `createLogger(write, now?)` | A `Logger` whose `info` and `error` call `write` with one JSON line: `{ time, level, msg, requestId?, ...fields }`. `time` is `now().toISOString()`; `requestId` is there only inside a request |

`AsyncLocalStorage` follows the async flow: code called from the handler, after any number of
`await`s, timers or callbacks, sees the same store, without passing the id around.

## 2. createService(handler, logger)

Returns `{ server, ready: true }`, where `server` is an `http.Server`, not listening yet.

- **Request id.** Use the request's `x-request-id` header if it matches
  `/^[A-Za-z0-9_-]{1,64}$/`, otherwise a new `randomUUID()` (never trust a header to be safe to
  log). Send it back in the `x-request-id` response header.
- **Context.** Everything for the request, the handler included, runs inside
  `requestContext.run({ requestId }, ...)`.
- **`GET /ready`** is answered by the service: **200** `{ "ready": true }`, or **503**
  `{ "ready": false }` once shutdown has started. Load balancers and Kubernetes use it.
- **Any other request** goes to `handler(req, res)`, which may return a promise.
- **Errors.** If the handler throws or rejects, log `"request failed"` with
  `{ error: <message> }` and answer **500** `{ "error": "internal error", "requestId": "..." }`:
  the id lets a user report the problem, and the details stay in the log.
- **Access log.** When the response is finished, log `"request"` with `method`, `path`
  (`req.url`), `status` and `ms`.
- While shutting down, answer with `connection: close`, so clients do not keep connections
  open.

## 3. installShutdown(service, options)

Listens for `SIGTERM` and `SIGINT` on `options.signals` (default `process`) and returns
`{ shutdown(reason) }`, which starts the same sequence by hand:

1. Log `"shutting down"` with `{ reason }` and set `service.ready = false`.
2. `server.close()`: no new connections; its callback runs when every connection is closed.
3. `server.closeIdleConnections()`: idle keep-alive connections close now; busy ones close
   after their response.
4. When `close` calls back, the shutdown is complete: `exit(0)`.
5. If that takes longer than `timeoutMs` (default 10 s), `server.closeAllConnections()` and
   `exit(1)`.
6. A second signal during shutdown means "now": close everything and `exit(1)`.

`shutdown()` resolves with the exit code, `exit` is called exactly once, and calling
`shutdown()` again returns the same promise. `exit` defaults to `process.exit`; the tests pass
their own `exit` and their own `EventEmitter` for the signals, so they never stop the test
process.

Run the tests with `npm test` after `npm install`. There are 12 tests: 1 in
`tests/visible.test.ts` and 11 hidden ones.
