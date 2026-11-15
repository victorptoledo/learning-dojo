# Parallel Fetch with Context

Call a slow service for many URLs at once, with a concurrency limit, a timeout per call,
cancellation on the first failure and a "fastest answer wins" mode. You get a `Fetcher`, a
function that respects its context: it returns `ctx.Err()` as soon as `ctx` is done.

The tests run under the race detector inside `testing/synctest` bubbles, so timings are exact
(a call of `100ms` takes exactly 100 ms of fake time) and **a test fails if any goroutine is
still blocked when it ends**.

## All

```go
func All(ctx context.Context, urls []string, limit int, perCall time.Duration, fetch Fetcher) ([]string, error)
```

- At most `limit` calls run at once (fewer than 1 means 1); 9 calls of 100 ms with a limit of
  3 take 300 ms.
- Each call gets its **own** timeout of `perCall`, derived from `ctx`.
- `bodies[i]` is the body of `urls[i]`, whatever order the calls finish in.
- The **first failing call cancels the others** (running calls see their context end, and no
  new call starts). `All` then returns `nil` and that failure as
  `fmt.Errorf("fetch %s: %w", url, err)`.
- If `ctx` itself ends (a deadline or a cancel from the caller), `All` stops and returns an
  error that wraps `ctx`'s error.

## First

```go
func First(ctx context.Context, urls []string, fetch Fetcher) (string, error)
```

- Calls every URL at once and returns the **first successful** body, then cancels the other
  calls; their goroutines must still be able to finish (no blocked sends).
- When every call fails, returns `errors.Join` of all the errors, each as
  `"fetch URL: cause"`. With no URLs, returns `ErrNoURLs`.

Tools: `context.WithCancel`, `context.WithCancelCause` and `context.Cause`,
`context.WithTimeout`, a buffered channel as a semaphore or as a results channel with room
for everyone, `sync.WaitGroup`, `select` and `errors.Join`.

Run the visible test with `go test -race`. The dojo runs 10 tests: 1 in `fetch_test.go` and 9
hidden ones, plus `go vet` and `gofmt` on `fetch.go`.
