# Hello, HTTP

`ArticleStore` (two articles in memory) and the records are written. Write `ArticleController`.
An article's **ETag** is its version in quotes: version 1 → `"v1"`.

## GET /articles/{id}

- `200` with the article as JSON, the header `ETag: "v1"` and `Cache-Control: max-age=60`.
- If the request has `If-None-Match` equal to the current ETag: **`304 Not Modified`**, with the
  `ETag` header and **no body**. The client keeps using its cached copy.
- Unknown id: `404`.
- `HEAD /articles/{id}` must answer the same headers without a body. Spring does this for you
  for every `GET` mapping (the server drops the body); the test checks the headers.

## PUT /articles/{id} (JSON body `{"title": "...", "body": "..."}`)

- Saves a new version (`store.update`) and answers `200` with the new article and its new `ETag`.
- If the request has `If-Match` and it is **not** the current ETag: **`412 Precondition Failed`**
  and nothing changes. This stops a client from overwriting a change it has not seen.
- Unknown id: `404`. Only `application/json` bodies are accepted: anything else is `415`
  (declare `consumes`, Spring answers the 415).

## Not supported

`DELETE /articles/1` must be `405 Method Not Allowed` with an `Allow` header. Spring does that
when no mapping accepts the method: just do not map `DELETE`.

> Tip: `@RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch`,
> `ResponseEntity.ok().eTag(etag).cacheControl(CacheControl.maxAge(Duration.ofSeconds(60))).body(...)`,
> `ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build()`, and
> `@PutMapping(value = "...", consumes = MediaType.APPLICATION_JSON_VALUE)`.
