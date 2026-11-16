# Choosing HTTP Status Codes

The status code is the first thing a client reads and often the only thing generic code
(retry policies, circuit breakers, monitoring, SDKs) looks at. Choose it for those readers:
the **class** (the first digit) decides what generic code does; the exact code refines it for
code that cares.

| Class | Meaning | What generic clients do |
| --- | --- | --- |
| `2xx` | success | use the body |
| `3xx` | go elsewhere / use your copy | follow `Location`, or use the cached copy on `304` |
| `4xx` | **the client** must change something | do **not** retry the same request unchanged (except `408`, `429`) |
| `5xx` | **the server** failed | may retry later, ideally with backoff, if the request is idempotent |

The most damaging mistake is `200 OK` with `{"error": "..."}` in the body: monitoring counts it
as success, caches may store it, retries never happen, and every client has to parse bodies to
learn that something failed. The second is `500` for bad input, which pages the on-call
engineer for a client's typo.

## The codes you will use

| Code | Use it when | Notes |
| --- | --- | --- |
| `200 OK` | a read or an update returns a body | |
| `201 Created` | a new resource was created | send `Location` with its URL, usually the resource as body |
| `202 Accepted` | work was queued and will finish later | return a status resource the client can poll (phase 3) |
| `204 No Content` | success with nothing to say (`DELETE`, some `PUT`s) | no body at all |
| `304 Not Modified` | a conditional `GET` and the client's copy is current | phase 2 |
| `400 Bad Request` | the request is malformed or invalid (syntax, validation) | say which fields |
| `401 Unauthorized` | no or invalid credentials | send `WWW-Authenticate`; the name means *unauthenticated* |
| `403 Forbidden` | authenticated, but not allowed | |
| `404 Not Found` | no such resource (or you do not want to reveal it exists) | |
| `405 Method Not Allowed` | the resource exists, the method does not apply | send `Allow: GET, PUT` |
| `409 Conflict` | conflicts with the current **state** of the resource | duplicate, wrong state transition |
| `410 Gone` | existed, removed on purpose, will not come back | sunset endpoints |
| `412 Precondition Failed` | an `If-Match` (or other precondition) did not hold | phase 2 |
| `415 Unsupported Media Type` | the request body's `Content-Type` cannot be read | |
| `422 Unprocessable Content` | syntactically fine, but violates a business rule | many APIs use `400` for all validation; pick one and be consistent |
| `428 Precondition Required` | you require `If-Match` and it is missing | RFC 6585 |
| `429 Too Many Requests` | rate limit exceeded | send `Retry-After` (phase 3) |
| `500 Internal Server Error` | an unexpected bug | never leak the stack trace |
| `502 / 504` | an upstream failed or timed out (gateways and proxies) | |
| `503 Service Unavailable` | temporarily overloaded or in maintenance | send `Retry-After` |

## Decision questions

1. **Did it work?** `2xx`. Created something? `201` + `Location`. Nothing to return? `204`.
   Will it finish later? `202`.
2. **Who has to act?** The client (fix the request, authenticate, wait) → `4xx`. The server →
   `5xx`.
3. **Which `4xx`?** Cannot parse or invalid input → `400`/`422`. Who are you? → `401`. You may
   not → `403`. Not there → `404`. Not like that → `405`. Not now, because of the state → `409`.
   Not this version → `412`. Slow down → `429`.

`401` vs `403` vs `404`: when revealing that a resource exists would leak information (another
tenant's invoice), answer `404` to unauthorized callers too. That is a security decision; write
it down so every endpoint makes the same one.

## Consistency beats perfection

Debates about `400` vs `422` or `409` vs `422` have no universal winner. What hurts clients is
an API where two endpoints answer the same situation differently. Put the rules in your style
guide (phase 4) and in a shared error handler (phase 1).

## Further reading

- RFC 9110, section 15, "Status Codes": <https://www.rfc-editor.org/rfc/rfc9110#section-15>
- RFC 6585, "Additional HTTP Status Codes" (428, 429, 431, 511): <https://www.rfc-editor.org/rfc/rfc6585>
- IANA HTTP Status Code Registry: <https://www.iana.org/assignments/http-status-codes/http-status-codes.xhtml>
- Zalando RESTful API Guidelines, "HTTP status codes": <https://opensource.zalando.com/restful-api-guidelines/#http-status-codes-and-errors>
- MDN Web Docs, "HTTP response status codes": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status>
