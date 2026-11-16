# Checkpoint 2: Inventory Service

A warehouse uploads its stock as CSV files with hundreds of thousands of rows. Write the
service that imports them **as a stream**, line by line as the bytes arrive, and exports the
inventory back as NDJSON, also streamed. It brings the phase together: streams, `readline`,
`pipeline`, an HTTP server, status codes and central error handling.

```text
sku,name,qty
AB-12,Blue pen,40
CD-7,Notebook,12
```

Write four functions in `inventory.ts`. `server.ts` starts the service with `npm start`.

| Function | Does |
| --- | --- |
| `parseRow(line)` | One data row: exactly three comma-separated fields, each trimmed. `sku` matches `/^[A-Z0-9-]{3,20}$/`, `name` is not empty, `qty` is a whole number of at least 0. Returns `{ ok: true, item }` or `{ ok: false, reason }` |
| `importCsv(source, inventory)` | Reads the CSV from a readable stream **line by line** with `readline` (`\r\n` and lines split across chunks included) into the `inventory` map, and resolves with `{ imported, rejected }` (see below) |
| `exportNdjson(inventory)` | A `Readable` that produces one `JSON.stringify(item) + "\n"` per item, sorted by sku |
| `createInventoryServer(inventory?)` | The HTTP service (see below), not listening yet |

## importCsv

- Blank lines are skipped. The first non-blank line must be the header `sku,name,qty` (any
  case, spaces around the commas allowed); otherwise, or when there is no line at all, it
  throws `HttpError` **400**.
- A valid row inserts the item or **replaces** the one with the same sku, and counts in
  `imported`.
- An invalid row is skipped and reported in `rejected` as `{ line, reason }`, where `line` is
  the 1-based line number in the file (the header and blank lines count).
- It must never hold the whole file in memory: one test uploads 20,000 rows in chunks.

## Routes

| Method and path | Answer |
| --- | --- |
| `POST /import` with `content-type: text/csv` | **200** with the report `{ imported, rejected }` as JSON. The request is the stream: pass it to `importCsv` |
| `GET /items` | **200** with `content-type: application/x-ndjson`, streamed from `exportNdjson` with `pipeline` into the response |
| `GET /items/:sku` | **200** with the item as JSON, **404** if there is none |
| `POST /import` with another content type | **415** |
| A wrong method on these paths | **405** with `Allow` (`POST` for `/import`, `GET` for the others) |
| Any other path | **404** |

Errors are JSON `{ "error": "..." }`, sent from **one** `.catch` in the request listener: an
`HttpError` gives its status, anything else is logged and becomes **500**. When an error
happens before the body was read to the end, call `req.resume()` so the rest of the body is
thrown away and the client can read the answer. A small `sendJson(res, status, body)` helper,
like the one given in the Notes API kata, keeps the routes short.

Run the tests with `npm test` after `npm install`. There are 11 tests: 1 in
`tests/visible.test.ts` and 10 hidden ones.
