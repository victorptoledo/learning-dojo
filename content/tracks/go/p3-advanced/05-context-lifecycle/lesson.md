# Context: Cancellation, Deadlines and Propagation

A request arrives, your handler calls a database and two services, and the client hangs up
after 200 ms. Everything started for that request should **stop**. `context.Context` is how
Go carries that signal, a deadline and a few request-scoped values through every call and
goroutine of an operation.

## The interface

```go
type Context interface {
	Deadline() (deadline time.Time, ok bool)
	Done() <-chan struct{}      // closed when the work should stop
	Err() error                 // nil, context.Canceled or context.DeadlineExceeded
	Value(key any) any
}
```

Contexts form a **tree**. You derive a child from a parent; cancelling a parent cancels all
of its children, never the other way round.

```go
ctx := context.Background()                          // the root, in main and tests' setup

ctx, cancel := context.WithCancel(parent)            // cancelled by calling cancel()
ctx, cancel := context.WithTimeout(parent, 2*time.Second)
ctx, cancel := context.WithDeadline(parent, deadline)
defer cancel()                                       // always: it releases the timer and children
```

`context.TODO()` marks a place where a real context should be passed but is not yet.

## The conventions

- A function that does I/O or may block takes **`ctx context.Context` as its first
  parameter**: `func (s *Store) Find(ctx context.Context, id string) (User, error)`.
- **Pass it down** to everything that accepts one: `http.NewRequestWithContext`,
  `db.QueryContext`, `exec.CommandContext`, your own functions.
- **Do not store a context in a struct**; pass it per call. A context belongs to one
  operation.
- **Always call cancel**, usually with `defer`, even when the operation succeeds; `go vet`'s
  `lostcancel` check flags forgotten ones.
- In an HTTP server, `r.Context()` is cancelled when the client disconnects or the handler
  returns. In tests, `t.Context()` is cancelled when the test ends.

## Respecting cancellation

Library calls that take a context stop on their own. In your own loops and goroutines,
check `ctx.Done()`:

```go
func process(ctx context.Context, jobs <-chan Job) error {
	for {
		select {
		case <-ctx.Done():
			return ctx.Err()          // context.Canceled or context.DeadlineExceeded
		case job, ok := <-jobs:
			if !ok {
				return nil
			}
			if err := handle(ctx, job); err != nil {
				return err
			}
		}
	}
}

func sleepCtx(ctx context.Context, d time.Duration) error {
	t := time.NewTimer(d)
	defer t.Stop()
	select {
	case <-t.C:
		return nil
	case <-ctx.Done():
		return ctx.Err()
	}
}
```

Test the error with `errors.Is(err, context.DeadlineExceeded)` or
`errors.Is(err, context.Canceled)`: libraries wrap them.

### Causes

`context.WithCancelCause` lets you say **why** you cancelled, and `context.Cause(ctx)` reads
it; `WithTimeoutCause` and `WithDeadlineCause` do the same for timeouts:

```go
ctx, cancel := context.WithCancelCause(parent)
cancel(fmt.Errorf("fetch %s: %w", url, err))   // the first failure stops the others
...
context.Cause(ctx)                             // the error passed to cancel
```

Since Go 1.26, `signal.NotifyContext` cancels with a cause that names the signal received,
which makes shutdown logs clearer.

### Other helpers

| Function | Use |
| --- | --- |
| `context.AfterFunc(ctx, f)` | run `f` in its own goroutine when ctx is done (close a connection, wake a `sync.Cond`) |
| `context.WithoutCancel(ctx)` | keep the values but not the cancellation, for work that must finish after the request (an audit write) |
| `signal.NotifyContext(ctx, os.Interrupt, syscall.SIGTERM)` | a context cancelled on Ctrl+C or SIGTERM, for graceful shutdown |

## Values

`context.WithValue` carries **request-scoped data that crosses API boundaries**: a request
ID, a trace span, the authenticated user. It is **not** for optional parameters or
dependencies, which belong in function arguments and structs.

```go
type ctxKey struct{}                     // an unexported key type: no collisions

func WithRequestID(ctx context.Context, id string) context.Context {
	return context.WithValue(ctx, ctxKey{}, id)
}

func RequestID(ctx context.Context) (string, bool) {
	id, ok := ctx.Value(ctxKey{}).(string)
	return id, ok
}
```

## Contexts and goroutine leaks

A goroutine that waits on a channel **and** on `ctx.Done()` can always be stopped. Combined
with `defer cancel()` at the top of an operation, this is the standard way to make sure the
goroutines of a request end with the request:

```go
func fastest(ctx context.Context, urls []string) (string, error) {
	ctx, cancel := context.WithCancel(ctx)
	defer cancel()                           // tells the losers to stop
	results := make(chan string, len(urls))  // room for everyone: no blocked sends
	for _, url := range urls {
		go func() {
			if body, err := fetch(ctx, url); err == nil {
				results <- body
			}
		}()
	}
	select {
	case body := <-results:
		return body, nil
	case <-ctx.Done():
		return "", ctx.Err()
	}
}
```

## Summary

- `Context` carries cancellation, a deadline and request values; children are cancelled
  with their parent.
- First parameter, passed down, never stored, `defer cancel()` always.
- Check `ctx.Done()` in your loops and selects; compare errors with `errors.Is`.
- `WithCancelCause` and `Cause` say why; `AfterFunc`, `WithoutCancel` and
  `signal.NotifyContext` cover the special cases.
- Values only for request-scoped data, with unexported key types.

## Further reading

- Package context: <https://pkg.go.dev/context>
- Go Concurrency Patterns: Context (Go blog): <https://go.dev/blog/context>
- Contexts and structs (Go blog): <https://go.dev/blog/context-and-structs>
- Go Concurrency Patterns: Pipelines and cancellation (Go blog): <https://go.dev/blog/pipelines>
- roadmap.sh, Go topic "Context Package": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/context-package@vxB3aGreqWBrNhiu8hcPE.md>
