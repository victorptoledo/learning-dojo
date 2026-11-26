# Checkpoint 4: Catalog Gateway

The product service is slow (hundreds of milliseconds per call), sometimes fails, and falls
over when it gets too many requests at once. Every page of the shop needs it. Put a
**gateway** in front of it: a small `node:http` service that caches products, never calls the
upstream twice for the same product at the same time, answers from an old copy when the
upstream is down, refuses work it cannot take, follows each request through its logs and the
upstream, and stops without cutting requests in half.

It is the final exam of the track: HTTP and errors (P2), async code and `AbortSignal` (P1),
request context, logs, shutdown and performance (P4).

```text
$ curl -i localhost:3000/products/pen -H "x-request-id: order-42"
HTTP/1.1 200 OK
x-request-id: order-42
x-cache: miss
content-type: application/json

{"id":"pen","name":"Blue pen","price":2.5}
```

`gateway.ts` gives the types, `DEFAULTS`, `requestContext` (an `AsyncLocalStorage`) and the
two patterns `REQUEST_ID` and `PRODUCT_ID`. `main.ts` starts the gateway with `npm start`
(`UPSTREAM=http://127.0.0.1:4000`).

## 1. currentRequestId() and LruCache

`currentRequestId()` is the `requestId` of the current context, or `undefined` outside a
request.

`new LruCache<V>(maxEntries)` keeps at most `maxEntries` keys (`RangeError` unless it is a
whole number of at least 1). `get` and `set` make a key the **most recently used**; when `set`
goes over the limit, the **least recently used** key is dropped. `delete(key)`, `size` and
`keys()` (least recently used first) complete it. A `Map` keeps its insertion order, so
deleting and inserting again moves a key to the end.

## 2. createGateway(options)

Returns `{ server, ready, metrics(), shutdown(graceMs?) }`; the server is not listening yet.
Options and their defaults are in `GatewayOptions` and `DEFAULTS`. Use `options.now()` (default
`Date.now`) for the cache times: the tests move that clock by hand.

**Every request:** take `x-request-id` when it matches `REQUEST_ID`, otherwise a new
`randomUUID()`; send it back in the `x-request-id` header; and handle the request inside
`requestContext.run({ requestId }, ...)`. When the response is finished, write one access
log line through `options.log` (default: stdout), a JSON object with `time` (ISO), `level:
"info"`, `msg: "request"`, `requestId`, `method`, `path` (`req.url`), `status`, `ms` and, for
products, `cache` (`"hit"`, `"miss"` or `"stale"`). Every answer is JSON.

| Route | Answer |
| --- | --- |
| `GET /ready` | **200** `{ "ready": true }`, **503** `{ "ready": false }` once shutdown has started |
| `GET /metrics` | **200** with `metrics()` |
| `/products/:id` | See below. A method other than GET: **405** with `Allow: GET` |
| Anything else | **404** |

**`GET /products/:id`**, in this order:

1. **Load shedding.** When `maxInFlight` product requests are already in progress, answer
   **503** `{ "error": "overloaded" }` with `Retry-After: 1` at once. Otherwise this request
   counts as in progress until its response is closed.
2. **Validation.** The id, URL-decoded, must match `PRODUCT_ID`; otherwise **400**. A bad id
   never reaches the upstream (a malformed escape such as `%E0%A4%A` is a bad id too).
3. **Fresh hit.** A cached product younger than `ttlMs` is answered with **200** and
   `x-cache: hit`.
4. **Miss.** Call `GET <upstream>/products/<id>` with `fetch`, the request's id in the
   `x-request-id` header and `AbortSignal.timeout(timeoutMs)`. **Single flight:** while a call
   for an id is in progress, other misses for that id wait for the same call instead of making
   their own.
   - 200 with a JSON body: cache it, answer **200** with the same body and `x-cache: miss`.
   - 404: answer **404**, and do not cache it.
   - Anything else (another status, a body that is not JSON, a network error, the timeout):
     when the cache still has an **expired** copy, answer it with **200** and
     `x-cache: stale`. Otherwise **504** `{ "error": "upstream timeout", "requestId" }` for the
     timeout and **502** `{ "error": "upstream unavailable", "requestId" }` for the rest.

`metrics()` returns `{ requests, hits, misses, stale, shed, upstreamCalls, inFlight,
cacheEntries }`: every request received, fresh hits, product requests that were not a fresh
hit (each waiting request counts), stale answers, shed requests, real calls to the upstream,
product requests in progress and products in the cache.

## 3. shutdown(graceMs = 10000)

Sets `ready` to false, stops accepting connections (`server.close()`), closes idle keep-alive
connections and lets requests in progress finish (once `close()` was called, Node.js answers them
with `connection: close`, so clients do not reuse the connection).
After `graceMs`, whatever is still open is closed by force (`closeAllConnections()`). The
promise resolves when the server has closed; calling `shutdown` again returns the same promise.

Run the tests with `npm test` after `npm install`. There are 18 tests: 1 in
`tests/visible.test.ts` and 17 hidden ones. The tests start a fake product service on a random
port and control its answers, delays and failures.
