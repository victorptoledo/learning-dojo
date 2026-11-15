---
from: go
status: draft
concept: "Errors as values and goroutines"
from_way: "Errors are returned values checked with if err != nil, goroutines and channels for concurrency, context.Context for cancellation and deadlines."
native_way: "Exceptions plus promises by default; Result unions bring Go-style errors as values where failures are expected; AbortSignal plays the role of context.Context."
sensei_tip: "Go's discipline of errors as values transfers well: use a Result union for expected failures, and reserve throw for what Go would panic on or bubble up."
---

| Go | TypeScript |
| --- | --- |
| `func Load(id string) (User, error)` | `function load(id: string): Promise<Result<User>>` |
| `if err != nil { return err }` | `if (!result.ok) return result` |
| `go f()` + `sync.WaitGroup` | `Promise.all([...])` |
| `errgroup` with `SetLimit(10)` | A small worker pool |
| `context.WithTimeout` | `AbortSignal.timeout(ms)` |
| `fmt.Errorf("x: %w", err)` | `new Error("x", { cause: err })` |
