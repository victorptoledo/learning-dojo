# Conditional Requests: a Product Page That Revalidates

The product page polls `GET /api/products/:id` every few seconds, and the admin screen edits
products. Two problems: every poll downloads the whole product even when nothing changed, and
two admins editing the same product silently overwrite each other (the lost update). HTTP
already solves both with **validators**: an `ETag` on every answer, `If-None-Match` to revalidate
a cached copy (`304 Not Modified`), and `If-Match` to update only the version you have seen
(`412 Precondition Failed` otherwise).

## The server (`server.ts`)

`createProductServer(catalog)` returns a `node:http` server over a `Catalog` (written for you).

| Function / request | Does |
| --- | --- |
| `etagOf(body)` | A **strong** ETag for a JSON body: `"` + the first 16 characters of the base64url SHA-256 of the body + `"`. Same body, same ETag |
| `matches(header, etag)` | Whether an `If-None-Match` / `If-Match` header value matches: `*` matches anything; otherwise a comma-separated list of ETags, compared after removing a `W/` prefix (the weak comparison of RFC 9110) |
| `GET /api/products/:id` | `200`, the product as JSON, with `ETag` and `Cache-Control: no-cache` (the browser may store it but must revalidate). With an `If-None-Match` that matches: **`304`**, no body, and the same `ETag` |
| `PUT /api/products/:id` (JSON body: the fields to change, `name` and/or `priceCents`) | without `If-Match`: **`428`**; with one that does not match the current ETag: **`412`** and the current product (and its `ETag`) in the answer, so the client can show what changed; otherwise `200`, the updated product and its **new** `ETag` |
| unknown product | `404` |

## The client (`client.ts`)

`CachingClient` remembers, per path, the last body and its ETag:

| Method | Does |
| --- | --- |
| `get(path)` | Sends `If-None-Match` with the cached ETag when it has one. On `304` returns the cached body (and adds 1 to `stats.revalidated`); on `200` stores the new body and ETag (and adds 1 to `stats.downloaded`) |
| `update(path, changes)` | `PUT`s the changes as JSON with `If-Match: <cached ETag>` (call `get` first; without a cached copy, throw an `Error`). On `200` stores the new body and ETag and returns the body. On `412` stores what the server sent as the new cached copy and throws a `ConflictError` whose `current` is that product |

Any other status is an `Error` with the status in its message.

## How the kata is checked

`tsc` checks every file, then `node:test` runs the server on `127.0.0.1` and drives it with your
client and with plain `fetch`. There are 15 tests: 2 in `tests/visible.test.ts` and 13 hidden
ones. The functions that still throw `new Error("todo ...")` are yours to write.
