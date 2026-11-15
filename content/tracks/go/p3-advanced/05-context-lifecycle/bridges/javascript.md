---
from: javascript
status: draft
concept: "AbortController and AbortSignal"
from_way: "An AbortController whose signal is passed to fetch and other APIs, AbortSignal.timeout(ms), signal.reason, and AsyncLocalStorage for request-scoped data."
native_way: "context.Context is AbortSignal plus a deadline plus request values, and Go's standard library accepts it everywhere I/O happens."
sensei_tip: "If you already pass signal to every fetch, you have the Go habit: ctx is the first argument of everything that can wait."
---

| JavaScript | Go |
| --- | --- |
| `const c = new AbortController()` | `ctx, cancel := context.WithCancel(parent)` |
| `c.abort(reason)` | `cancel(cause)` with `WithCancelCause` |
| `AbortSignal.timeout(2000)` | `context.WithTimeout(ctx, 2*time.Second)` |
| `fetch(url, { signal })` | `http.NewRequestWithContext(ctx, ...)` |
| `signal.addEventListener("abort", f)` | `context.AfterFunc(ctx, f)` |
| `signal.reason` | `context.Cause(ctx)` |
| `AsyncLocalStorage` | `context.WithValue` |
| `AbortSignal.any([a, b])` | derive from one parent; children follow it |
