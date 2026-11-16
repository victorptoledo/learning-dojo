# HTTP Semantics: Methods, Safety, Idempotency and Headers

HTTP is not just a transport for JSON. RFC 9110 gives every method and header a **meaning**
that clients, proxies, caches, load balancers and SDKs rely on without reading your docs. A
design that respects those meanings gets retries, caching and tooling for free; a design that
ignores them (everything is `POST /api`) loses all of it.

> The Backend track's lessons "Hello, HTTP" (`backend.p0.hello-http`) and "HTTP in Depth"
> (`backend.p1.protocols`) cover the protocol itself (connections, HTTP/2 and HTTP/3). This
> lesson is about the semantics an API designer must choose correctly.

## Methods and their properties

| Method | Meaning | Safe | Idempotent | Body in request |
| --- | --- | --- | --- | --- |
| `GET` | transfer a representation of the target resource | yes | yes | no (undefined semantics) |
| `HEAD` | like `GET` without the body | yes | yes | no |
| `OPTIONS` | what the target supports (CORS preflight uses it) | yes | yes | rarely |
| `POST` | process the enclosed content according to the resource's own rules (create a child, run an action) | no | **no** | yes |
| `PUT` | replace the target's state with the enclosed representation (create it if absent) | no | yes | yes |
| `PATCH` | apply a partial modification (RFC 5789) | no | not guaranteed | yes |
| `DELETE` | remove the association between the target and its state | no | yes | rarely |

- **Safe** means the client does not request any state change: crawlers, prefetchers and
  monitoring may call it freely. A `GET /orders/42/cancel` that cancels the order is a bug
  waiting for a link preview bot.
- **Idempotent** means sending the request twice has the same intended effect as sending it
  once. That is what makes automatic retries legal: an HTTP client or proxy may retry a `PUT`
  or `DELETE` after a timeout, never a plain `POST`. Idempotency is about the **effect on the
  server**, not the response: the second `DELETE` may answer `404` and still be idempotent.

`PATCH` is idempotent only if its document format makes it so: JSON Merge Patch (RFC 7396)
`{"status": "PAID"}` is, a JSON Patch (RFC 6902) operation `add` to an array is not.

## Representations and content negotiation

A resource is a concept (*order 42*); what travels is a **representation** of it, described by
headers:

| Header | Direction | Example |
| --- | --- | --- |
| `Content-Type` | both | `application/json`, `application/problem+json` |
| `Accept` | request | `application/json, text/csv;q=0.5` |
| `Accept-Language` | request | `pt-BR, en;q=0.8` |
| `Content-Encoding` / `Accept-Encoding` | both | `gzip`, `br` |
| `Vary` | response | `Accept, Accept-Language`: caches must key on these too |

If the server cannot produce any acceptable type, it answers `406 Not Acceptable`; if it cannot
read the request's type, `415 Unsupported Media Type`.

## Headers an API designer uses every day

| Header | Purpose | Covered in |
| --- | --- | --- |
| `Location` | URL of a created resource (with `201`) or of a redirect | phase 1 |
| `ETag`, `If-None-Match`, `If-Match` | validators and conditional requests | phase 2 |
| `Cache-Control` | who may store the response and for how long | phase 2 |
| `Retry-After` | when to try again (`429`, `503`) | phases 2 and 3 |
| `Authorization`, `WWW-Authenticate` | credentials and the challenge for them | phase 3 |
| `Link` | relations to other resources: `next`, `successor-version` (RFC 8288) | phases 1 and 2 |

Custom headers do not need an `X-` prefix (RFC 6648 deprecated it); a well-named header such as
`Idempotency-Key` is better than `X-Idempotency-Key`.

## Statelessness

Each request must carry everything the server needs to understand it (credentials, the cursor
of the page, the version). No "current page" stored in a server session. Stateless requests can
go to any instance behind a load balancer and can be retried, cached and logged on their own.

## Further reading

- RFC 9110, "HTTP Semantics", section 9 (methods) and 9.2 (common method properties): <https://www.rfc-editor.org/rfc/rfc9110#section-9>
- RFC 5789, "PATCH Method for HTTP": <https://www.rfc-editor.org/rfc/rfc5789>
- RFC 7396, "JSON Merge Patch": <https://www.rfc-editor.org/rfc/rfc7396>
- RFC 6648, "Deprecating the X- Prefix": <https://www.rfc-editor.org/rfc/rfc6648>
- MDN Web Docs, "HTTP request methods": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Methods>
