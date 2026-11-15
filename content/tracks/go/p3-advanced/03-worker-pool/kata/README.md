# Worker Pool and Pipeline

Write a bounded worker pool and a three-stage channel pipeline in `pool.go`. The tests run
under the **race detector** and inside `testing/synctest` bubbles: `time.Sleep` uses a fake
clock, so timing checks are exact, and a test **fails if any goroutine is left blocked** when
it ends (a leak or a channel nobody closed).

## Map

```go
func Map[In, Out any](jobs []In, workers int, fn func(In) (Out, error)) ([]Out, error)
```

- Calls `fn` on every job with **at most `workers` calls running at once** (fewer than 1
  means 1), and really in parallel: 20 jobs of 10 ms on 4 workers take 50 ms.
- `results[i]` is what `fn` returned for `jobs[i]`, **whatever order the jobs finish in**.
  A failed job leaves the zero value.
- **Every job runs**, even after a failure. The error is `errors.Join` of
  `fmt.Errorf("job %d: %w", i, err)` for each failing job, **in job order** (`nil` when
  nothing failed).
- No data races: two goroutines must never write the same variable without
  synchronisation. Writing different elements of a slice is fine; appending to a shared
  slice is not.

## The pipeline

| Function | Does |
| --- | --- |
| `Generate(nums ...int) <-chan int` | Sends `nums` in order on a new channel from a goroutine, then **closes** it |
| `Square(in <-chan int, workers int) <-chan int` | Starts `workers` goroutines (at least 1) that all read `in` and send `n*n`; the output is closed **after the last worker finishes** |
| `Sum(in <-chan int) int` | Adds every value until `in` is closed |

```go
pool.Sum(pool.Square(pool.Generate(1, 2, 3, 4), 2)) // 30
```

Tools: `sync.WaitGroup` and `wg.Go`, a channel of job indexes (or a semaphore channel),
`errors.Join`, `close`, and `for v := range ch`.

Run the visible test with `go test -race`. The dojo runs 10 tests: 1 in `pool_test.go` and 9
hidden ones, plus `go vet` and `gofmt` on `pool.go`.
