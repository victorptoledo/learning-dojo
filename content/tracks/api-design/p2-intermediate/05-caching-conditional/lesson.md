# HTTP Caching and Conditional Requests (ETag)

HTTP caching (RFC 9111) is the cheapest performance feature you will ever ship: a response that
is not sent costs no CPU, no database query and no bandwidth. The same machinery, **validators**
and **conditional requests**, also solves a correctness problem: two clients editing the same
resource without overwriting each other.

## Who caches

- the **browser** or mobile HTTP client (a *private* cache);
- **shared caches**: CDNs, reverse proxies, corporate proxies;
- your own gateway.

`Cache-Control` on the response tells all of them what they may do.

| Directive | Meaning |
| --- | --- |
| `max-age=60` | fresh for 60 s: reuse without asking |
| `s-maxage=300` | the same, for shared caches only |
| `no-cache` | may store, but must **revalidate** before every use |
| `no-store` | must not store at all (secrets, personal data) |
| `private` | only the end user's cache may store it (personalised responses) |
| `public` | shared caches may store it even if the request had `Authorization` |
| `stale-while-revalidate=30` | may serve a stale copy for 30 s while refreshing in the background |
| `immutable` | will never change during its freshness (fingerprinted assets) |

Responses to requests with `Authorization` are not stored by shared caches unless you say
`public` or `s-maxage`. Use `Vary` for every request header that changes the representation
(`Accept`, `Accept-Language`).

## Validators

| Validator | Example | Notes |
| --- | --- | --- |
| `ETag` (strong) | `ETag: "v7"` | the bytes are identical whenever the tag is |
| `ETag` (weak) | `ETag: W/"v7"` | semantically equivalent, maybe not byte-identical (different compression) |
| `Last-Modified` | `Last-Modified: Wed, 01 Apr 2026 12:00:00 GMT` | one-second resolution; weaker than an ETag |

An ETag can be a version number, a hash of the representation, or both. It must change when
the representation changes, and two representations of the same resource (JSON v1 and v2, two
languages) need **different** ETags.

## Conditional reads: 304 Not Modified

```http
GET /documents/readme
If-None-Match: "v7"

HTTP/1.1 304 Not Modified
ETag: "v7"
Cache-Control: private, no-cache
```

The client sends the tag it has; if it still matches, the server answers `304` without a body
and the client uses its copy. `If-None-Match` uses the **weak** comparison (`W/"v7"` matches
`"v7"`) and accepts a list and `*`. `If-Modified-Since` does the same with dates and is ignored
when `If-None-Match` is present.

A server that computes the ETag by hashing the full response (Spring's
`ShallowEtagHeaderFilter`) saves bandwidth but not work; a version column lets it answer `304`
without building the body.

## Conditional writes: no more lost updates

Alice and Bob both read document version 7. Alice saves; Bob saves a moment later and
silently overwrites Alice's change. This is the **lost update**. Conditional writes are
optimistic concurrency control over HTTP:

```http
PUT /documents/readme
If-Match: "v7"
Content-Type: application/json

{"title": "...", "body": "..."}
```

| Situation | Answer |
| --- | --- |
| `If-Match` matches the current ETag (strong comparison) or is `*` | apply the change, `200` with the new ETag |
| it does not match | `412 Precondition Failed`: read again, merge, retry |
| no `If-Match` and the API requires one | `428 Precondition Required` (RFC 6585) |

`If-Match` uses the **strong** comparison: a weak tag never matches, because "roughly the same"
is not good enough to decide a write. `If-None-Match: *` on a `PUT` means "create only if it
does not exist yet", a neat way to make creation idempotent.

The same idea shows up in other styles: Kubernetes' `resourceVersion`, DynamoDB conditional
writes, `version` columns with JPA's `@Version` (the Spring Boot track,
`spring-boot.p2.data-jpa`).

## Further reading

- RFC 9111, "HTTP Caching": <https://www.rfc-editor.org/rfc/rfc9111>
- RFC 9110, section 13, "Conditional Requests", and 8.8, "Validator Fields": <https://www.rfc-editor.org/rfc/rfc9110#section-13>
- RFC 6585, section 3, "428 Precondition Required": <https://www.rfc-editor.org/rfc/rfc6585#section-3>
- MDN Web Docs, "HTTP caching": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Caching>
- Google AIP-154, "Resource freshness validation" (etags): <https://google.aip.dev/154>
- Spring Framework reference, "HTTP Caching": <https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-caching.html>
