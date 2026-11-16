# Conditional Requests

The documents API works, but it wastes and loses: every `GET` sends the whole document even when
the client already has it, and when two people edit the same document, the second `PUT`
silently overwrites the first (the **lost update**). HTTP solves both with validators and
conditional requests (RFC 9110, section 13).

`Document`, `DocumentRequest`, `DocumentStore` (every change raises `version` and moves
`updatedAt`), `DocumentExceptions` and `ApiProblems` are written. Change `DocumentController`.

## Validators on every representation

`GET`, `POST`, and a successful `PUT` answer with:

| Header | Value |
| --- | --- |
| `ETag` | a **strong** entity tag that changes with every version, for example `"3"` |
| `Last-Modified` | `updatedAt` as an HTTP date |
| `Cache-Control` | `no-cache, private` (store it, but revalidate before each use) |

## Conditional reads

| Request | Answer |
| --- | --- |
| `If-None-Match` lists the current ETag (also as `W/"..."`, or `*`) | `304 Not Modified`, no body, with the `ETag`, `Last-Modified` and `Cache-Control` |
| `If-None-Match` with other tags only | `200` and the document |
| no `If-None-Match`, `If-Modified-Since` not before `updatedAt` | `304` |
| `If-None-Match` present | `If-Modified-Since` is ignored |

`If-None-Match` uses the **weak** comparison: `W/"3"` matches `"3"`.

## Conditional writes (`PUT`, `DELETE`)

| Request | Answer |
| --- | --- |
| no `If-Match` | `PreconditionRequiredException` → `428 Precondition Required` (RFC 6585) |
| `If-Match` lists the current ETag, or is `*` | the change happens: `200` with the new ETag (`PUT`), `204` (`DELETE`) |
| otherwise | `PreconditionFailedException` → `412 Precondition Failed`, nothing changes |

`If-Match` uses the **strong** comparison: a weak tag (`W/"3"`) never matches.

```text
GET /documents/readme              → 200, ETag: "1"
PUT /documents/readme  If-Match: "1"  → 200, ETag: "2"
PUT /documents/readme  If-Match: "1"  → 412 (somebody changed it since you read "1")
GET /documents/readme  If-None-Match: "2" → 304
```

> Tip: take `@RequestHeader HttpHeaders headers` and use `headers.getIfNoneMatch()`,
> `headers.getIfMatch()` (both parse lists) and `headers.getIfModifiedSince()` (`-1` when
> absent). `ResponseEntity` builders have `.eTag(...)`, `.lastModified(instant)` and
> `.cacheControl(CacheControl.noCache().cachePrivate())`.
