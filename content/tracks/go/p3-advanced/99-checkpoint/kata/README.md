# Checkpoint 3: The Batcher

Services rarely write one row or send one event at a time: they collect items from many
goroutines and flush them in **batches**, one insert of 500 rows instead of 500 inserts.
Build that batcher in `batch.go`. The types and signatures are declared; the fields of
`Batcher` and the functions are yours.

```go
b := batch.Start(ctx, batch.Config{Size: 500, Wait: 200 * time.Millisecond},
	func(ctx context.Context, rows []Row) error { return db.InsertMany(ctx, rows) })
defer b.Close()

// from any number of handler goroutines:
if err := b.Add(r.Context(), row); err != nil { ... }
```

| Function or method | Does |
| --- | --- |
| `Start(ctx, cfg, flush)` | Starts **one** goroutine that collects items and calls `flush`. A `Size` below 1 means 1. |
| `Add(ctx, item)` | Hands the item to that goroutine and returns `nil` once it holds it, in the order of the calls. While a flush runs, `Add` **waits** (backpressure). If the caller's `ctx` ends first, it returns `ctx.Err()` and the item is not added. After `Close`, or after the `Start` context ended, it returns `ErrClosed`. |
| flushing | A batch is flushed as soon as it holds `Size` items, or `Wait` after its **first** item was added (later items do not restart the clock; `Wait <= 0` means no timer). Flushes run one at a time, from the batcher's goroutine, in order. |
| the slice | Each slice given to `flush` **belongs to it**: the function may keep it, so the batcher must never write into it again. |
| errors | A failing flush does not stop the batcher. Each error is wrapped as `"batch N: ..."` (batches count from 1) and `Close` returns `errors.Join` of all of them. |
| `Close()` | Stops accepting items, flushes what is left, waits for the goroutine to return and returns the errors. Safe to call again, and from several goroutines: every call returns the same result. It must never panic, even while other goroutines are calling `Add`. |
| the `Start` context | When it ends, the goroutine flushes what it holds and returns, without waiting for `Close`. That last flush gets a context that is **not** cancelled (`context.WithoutCancel`), so the last batch is not lost. |
| `Stats()` | Items handed to `flush`, flush calls and failed calls; safe to read from any goroutine at any time. |

Every item whose `Add` returned `nil` is flushed exactly once before `Close` returns, and no
goroutine is left behind.

Tools: an unbuffered channel for the items, a channel you **close** (never send on) to say
"stop", `sync.Once`, `select` over several channels, `time.NewTimer` and its `Stop`,
`context.WithoutCancel`, `atomic.Int64` and `errors.Join`.

Run the visible test with `go test -race`. The dojo runs 12 tests: 1 in `batch_test.go` and
11 hidden ones, several inside `testing/synctest` bubbles, where time is fake and a goroutine
still blocked at the end fails the test. It also runs `go vet` and `gofmt` on `batch.go`.
