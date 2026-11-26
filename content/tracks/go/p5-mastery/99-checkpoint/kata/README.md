# 🥋 Final Exam: The Shortlinks Service

The final exam of the Go track is a small service you could ship: a link shortener on
`net/http` that validates its input, gives every store call a deadline, never leaks an
internal error to a client, logs one structured record per request, survives a panic and
shuts down without dropping a request. Write it in `shortlinks.go`. The types and
signatures are declared; the fields of `Server` and everything else are yours.

```go
srv := shortlinks.New(shortlinks.Config{NewCode: randomCode, Logger: logger}, store)
ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGTERM, os.Interrupt)
defer stop()
ln, _ := net.Listen("tcp", ":8080")
err := srv.Run(ctx, ln, shortlinks.RunOptions{DrainDelay: 5 * time.Second, ShutdownTimeout: 20 * time.Second})
```

## Routes

| Request | Answer |
| --- | --- |
| `POST /links` with `{"url": "https://..."}` | `201` with `Location: /{code}` and `{"code": "...", "url": "..."}`. The body is at most `MaxBody` bytes (`413` beyond). It must be exactly that JSON object: no unknown keys, no other types (`400`). The URL must be absolute, `http` or `https`, with a host (`400`). A code comes from `NewCode`; when the store says `ErrExists`, try a new one, **3 attempts** in all, then `500`. |
| `GET /{code}` | `302` to the stored URL, or `404` when the store says `ErrNotFound`. |
| `GET /healthz` | `200` while the process runs. |
| `GET /readyz` | `200`, or `503` while the server drains. |
| any other method on a route | `405`, from the mux's method patterns. |

Every error answer is JSON, `{"error": "..."}` with `Content-Type: application/json`.

## Production rules

| Rule | Detail |
| --- | --- |
| store deadlines | Each store call gets a context with `StoreTimeout`, **derived from the request's context**, so a client that leaves also cancels the call. A call that runs out of time answers `504`. |
| errors | Any other store error answers `500` with a generic message; the real error goes to the log only (it may hold a password or a SQL statement), at `ERROR` with the key `error` and the `request_id`. |
| request IDs | An incoming `X-Request-ID` of 1 to 64 letters, digits or `-` is kept; anything else (or nothing) is replaced by `NewRequestID()`, by default `req-1`, `req-2`... The ID is echoed in the response header and is in every log record of the request. |
| access log | One record per request, message `request`, with `request_id`, `method`, `pattern` (the route that matched, such as `GET /{code}`), `path` (never the query string: it may hold tokens), `status` and `duration`. Level `ERROR` for a 5xx, `INFO` otherwise. |
| panics | A panic in a handler answers `500` JSON and is logged at `ERROR` with the message `panic`; the server goes on. The access log must still see the `500`. |
| `Run(ctx, ln, opts)` | Serves on `ln`. When `ctx` ends: `/readyz` starts answering `503` while everything else is still served for `DrainDelay`, so the load balancer can take the instance out; then `Shutdown` waits up to `ShutdownTimeout` for in-flight requests and returns `nil` when they finish, or an error wrapping `context.DeadlineExceeded` when they do not. Ending `ctx` must **not** cancel the requests in flight. If serving fails (a closed listener), `Run` returns that error at once. Set the server timeouts you learned (`ReadHeaderTimeout` at least). |
| defaults | `Logger` nil discards logs, `StoreTimeout` 0 is 2 s, `MaxBody` 0 is 1024 bytes. |

Tools: `http.ServeMux` patterns and `r.PathValue`, `r.Pattern` (set by the mux on the
request), `http.MaxBytesReader` and `*http.MaxBytesError`, `json.Decoder`, `url.Parse`,
`context.WithTimeout`, `context.WithValue` with a private key type, a `ResponseWriter`
wrapper that remembers the status, `slog.Logger.LogAttrs`, `recover`, `atomic.Bool`,
`http.Server` with `BaseContext`, `Serve` and `Shutdown`.

Run the visible test with `go test -race`. The dojo runs 14 tests: 1 in `shortlinks_test.go`
and 13 hidden ones (some start a real server on `127.0.0.1`), plus `go vet` and `gofmt` on
`shortlinks.go`.
