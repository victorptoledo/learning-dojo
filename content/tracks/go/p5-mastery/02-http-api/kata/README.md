# To-do API with net/http

Build a small JSON API in `api.go` with the standard library only: `http.ServeMux` patterns,
`encoding/json`, a store guarded by a mutex, and a panic-recovery middleware. `Todo`, the
`Store` struct and two helpers, `writeJSON` and `writeError`, are given.

## The store

`Store`'s methods (`Add`, `Get`, `List`, `Update`, `Delete`) are called by many request
goroutines at once: lock `s.mu` in each one. Its **zero value works** (`&api.Store{}`), so
create the map on first use. Ids start at 1 and are never reused. `List` is sorted by id.

## The routes

`NewHandler(store, logger)` registers these routes on an `http.NewServeMux()` and returns the
mux wrapped in `Recover`:

| Route | Answers |
| --- | --- |
| `GET /healthz` | 200 with the plain text body `ok` |
| `GET /todos` | 200 with a JSON array of every todo, sorted by id: `[]` when empty, never `null` |
| `POST /todos` | Body `{"title": "..."}`. The title is trimmed; empty, missing or bad JSON gives **400**. Otherwise **201**, a `Location: /todos/{id}` header and the new todo as JSON |
| `GET /todos/{id}` | 200 with the todo |
| `PATCH /todos/{id}` | Body with `title` and/or `done`: changes **only the fields present** (decode into pointer fields). An empty title or bad JSON gives 400. 200 with the updated todo |
| `DELETE /todos/{id}` | **204** with no body |

For every `{id}` route: an id that is not a positive integer gives **400**, an unknown id
**404**. Every error answer is JSON: `{"error": "todo 9 not found"}` with
`Content-Type: application/json` (that is what `writeError` does). Wrong methods (`POST
/healthz`, `PUT /todos/1`) must get **405** from the mux itself, with its `Allow` header:
register routes with a method in the pattern.

## Recover

`Recover(logger, next)` returns a handler that calls `next` and, if it panics, logs the panic
value with `logger.Error` and answers **500** `{"error": "internal error"}`: the client never
sees the panic message.

Tools: `mux.HandleFunc("GET /todos/{id}", ...)`, `r.PathValue("id")`, `strconv.Atoi`,
`json.NewDecoder(r.Body).Decode`, `http.MaxBytesReader`, pointer fields such as `*bool` for
optional JSON fields, `defer` with `recover()`, `slices.Sorted(maps.Keys(...))`.

Run the visible test with `go test -race`. The dojo runs 11 tests: 1 in `api_test.go` and 10
hidden ones (with 50 concurrent requests under the race detector), plus `go vet` and `gofmt`
on `api.go`.
