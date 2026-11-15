---
from: go
status: draft
concept: "Wrapping functions and deferred cleanup"
from_way: "Functions that take and return a handler (func(http.Handler) http.Handler middleware), closures for retries and timing, and defer for cleanup at function exit."
native_way: "Decorators with @ for the same wrapping, functools.wraps to keep metadata, and with blocks that clean up at the end of the block instead of the function."
sensei_tip: "Go middleware is a decorator without the @. Note the scope difference: defer runs at function return, a with block cleans up as soon as the block ends."
---

| Go | Python |
| --- | --- |
| `func Logged(h http.Handler) http.Handler` | `def logged(func): ... return wrapper` |
| `mux.Handle("/", Logged(h))` | `@logged` above the handler |
| `f, _ := os.Open(p); defer f.Close()` | `with open(p) as f:` |
| several `defer` calls (LIFO) | `ExitStack` (also LIFO) |
| `sync.Once` / a map cache | `@functools.cache` |
| `start := time.Now(); defer log(time.Since(start))` | a `@timed` decorator or `Timer` context manager |
