# HTTP in Depth: Methods, Status, Caching, HTTP/2 and HTTP/3

HTTP is the contract between your API and every client, proxy and cache on the way. Frameworks
hide its mechanics, but its **semantics** are yours to get right: what a method promises, what a
status code means, which headers let caches and clients do their job.

## A message, both ways

```text
GET /orders/42 HTTP/1.1                     HTTP/1.1 200 OK
Host: shop.example.com                      Content-Type: application/json
Accept: application/json                    ETag: "v7"
Authorization: Bearer eyJhbGciOi...         Cache-Control: private, max-age=60

                                            {"id": 42, "status": "SHIPPED"}
```

A request has a **method**, a **target**, **headers** and an optional **body**; a response has a
**status**, headers and an optional body. Everything else is built on that.

## Methods and what they promise

| Method | Safe | Idempotent | Use |
| --- | --- | --- | --- |
| `GET` | yes | yes | read a resource |
| `HEAD` | yes | yes | `GET` without the body (check existence, size, ETag) |
| `OPTIONS` | yes | yes | which methods are allowed; CORS preflight |
| `POST` | no | **no** | create, or run an action |
| `PUT` | no | yes | replace the resource at this URL |
| `PATCH` | no | not necessarily | change part of a resource |
| `DELETE` | no | yes | remove it |

- **Safe** means no change on the server: crawlers, prefetchers and caches may call it freely.
  Never change state in a `GET`.
- **Idempotent** means calling it twice has the same effect as once. Clients, proxies and retry
  libraries may repeat idempotent requests after a timeout. `POST` is not, which is why creating
  payments needs extra care (an idempotency key, later in this track).

## Status codes

| Class | Meaning | The ones you need |
| --- | --- | --- |
| `2xx` | success | `200 OK`, `201 Created` (+ `Location`), `202 Accepted` (queued for later), `204 No Content` |
| `3xx` | go elsewhere / use your copy | `301`/`308` moved permanently, `302`/`307` temporarily, `304 Not Modified` |
| `4xx` | the client must change something | `400`, `401`, `403`, `404`, `405`, `409`, `412`, `415`, `422`, `429 Too Many Requests` |
| `5xx` | the server failed | `500`, `502 Bad Gateway`, `503 Service Unavailable` (+ `Retry-After`), `504 Gateway Timeout` |

`307` and `308` keep the method and body on the redirect; `301` and `302` let clients change a
`POST` into a `GET`. A `4xx` means retrying the same request is pointless; a `503` with
`Retry-After` invites a retry later.

## Headers that matter

- **Content negotiation:** the client says what it can read (`Accept: application/json`), the
  server says what it sends (`Content-Type`). A body in a format the server does not accept is
  `415`; a requested format it cannot produce is `406`.
- **Authentication:** `Authorization: Bearer <token>` or `Basic <base64>`; a `401` comes with
  `WWW-Authenticate`.
- **Tracing:** `traceparent` carries the distributed trace context between services.
- **Forwarding:** behind proxies, `X-Forwarded-For`/`-Proto` or the standard `Forwarded` header
  tell the app the original client and scheme. Trust them only from your own proxies.

## Caching and conditional requests

HTTP caching saves bandwidth and latency at every layer: browser, CDN, reverse proxy.

```text
Cache-Control: public, max-age=3600        anyone may cache it for an hour
Cache-Control: private, max-age=60         only the user's browser, one minute
Cache-Control: no-store                    never store it (sensitive data)
Cache-Control: no-cache                    store, but revalidate before every use
```

**Validators** let a client ask "has it changed?" cheaply:

```text
GET /articles/7                            → 200, ETag: "v3", body
GET /articles/7  If-None-Match: "v3"       → 304 Not Modified, no body (still v3)
```

and let it update safely without losing someone else's change (**optimistic concurrency**):

```text
PUT /articles/7  If-Match: "v3"            → 200, ETag: "v4"
PUT /articles/7  If-Match: "v3"  (again)   → 412 Precondition Failed: someone changed it
```

An ETag is an opaque string the server chooses: a version number, a hash of the content. A
**weak** ETag (`W/"v3"`) means "semantically equivalent", enough for caching but not for
`If-Match`. `Last-Modified` with `If-Modified-Since` is the older, date-based variant.

## HTTP/1.1, HTTP/2, HTTP/3

| Version | Transport | What it changed |
| --- | --- | --- |
| HTTP/1.1 | TCP | text messages; one request at a time per connection, so browsers open several |
| HTTP/2 | TCP + TLS | binary frames, **multiplexing** many streams on one connection, header compression |
| HTTP/3 | **QUIC** (UDP) + TLS 1.3 | no TCP head-of-line blocking, faster handshakes, survives network changes |

The semantics (methods, status, headers) are the same in all three: your API does not change.
The load balancer or proxy usually speaks HTTP/2 or HTTP/3 to clients and HTTP/1.1 or HTTP/2 to
your service. gRPC runs on HTTP/2.

## Real-time: polling, SSE and WebSockets

- **Polling:** the client asks every few seconds. Simple, wasteful.
- **Server-Sent Events:** one long HTTP response that streams events from server to client
  (`text/event-stream`), reconnects automatically. Perfect for notifications and progress.
- **WebSockets:** an upgraded connection carrying messages **both ways**. For chat, games,
  collaborative editing.

Choose the simplest that fits: most "real-time" features only need server-to-client, so SSE.

## Summary

- Safe methods change nothing; idempotent ones can be repeated; `POST` is neither.
- Status codes are part of the contract: `201`+`Location`, `204`, `304`, `409`, `412`, `429`,
  `503`+`Retry-After`.
- `Accept`/`Content-Type` negotiate formats; `Authorization` and `traceparent` travel on every call.
- `Cache-Control` sets who may cache and for how long; ETags with `If-None-Match` revalidate and
  with `If-Match` prevent lost updates.
- HTTP/2 multiplexes on TCP, HTTP/3 runs on QUIC; semantics are unchanged. SSE for one-way
  streams, WebSockets for two-way.
