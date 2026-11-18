List the polls, newest first, one page at a time.

`GET /polls` answers `200` with `{ "items": [<poll>, ...], "nextCursor": "..." | null }`.

- **Order:** newest `createdAt` first; polls created in the same millisecond come in a fixed
  order of your choice (by id, for instance), so no poll is ever skipped or repeated.
- **`limit`:** the page size, a whole number from 1 to 100, 20 by default.
- **`status`:** `open` or `closed` keeps only those polls, judged by `now()` at the time of the
  request.
- **`cursor`:** where the next page starts. `nextCursor` is `null` on the last page (also when
  the page is exactly full and nothing is left), otherwise an opaque string the client sends
  back as `?cursor=` with the same `limit` and `status`.

The cursor must survive new data: when polls are created between two pages, the next page
still continues right after the last poll the client saw, without repeating any. An offset
(`?page=3`) cannot do that, because every new poll shifts the old ones. Put in the cursor what
you need to find "the polls after this one", and encode it (base64url of a small JSON, for
instance) so clients treat it as opaque.

When there is a next page, also send a `Link` header with its URL, keeping `limit` and
`status`: `Link: </polls?limit=2&status=open&cursor=...>; rel="next"`.

**Errors:** a bad `limit`, `status` or `cursor` (one you did not issue) is a `400` problem with
an `errors` list, one entry per bad parameter, its `path` being the parameter's name.

`/polls` now allows `GET, POST` (in that order in `Allow`).
