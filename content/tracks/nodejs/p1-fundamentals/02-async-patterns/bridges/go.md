---
from: go
status: draft
concept: "Waiting for many things at once"
from_way: "Goroutines and channels, sync.WaitGroup or errgroup to wait for several, context.Context for deadlines and cancellation, errors as values."
native_way: "Promises, Promise.all to wait for several, AbortSignal for deadlines and cancellation, exceptions that reject promises."
sensei_tip: "AbortSignal is Node's context.Context: pass it down through every call that may wait. errgroup.Group maps to Promise.all, and errgroup with SetLimit to a small worker pool."
---

| Go | Node.js |
| --- | --- |
| `go f()` | Call an async function (it starts at once) |
| `errgroup.Wait()` | `await Promise.all([...])` |
| `context.WithTimeout` | `AbortSignal.timeout(ms)` |
| `ctx.Done()` | `signal.addEventListener("abort", ...)` |
| `for v := range ch` | `for await (const v of iterable)` |
| `if err != nil` | `try`/`catch` around `await` |
