```go
func New(workers int) *Scheduler               // fewer than 1 means 1
func (s *Scheduler) Submit(job Job) error      // the job starts as soon as a worker is free
func (s *Scheduler) Wait() map[string]Result   // blocks until every submitted job is finished
```

- At most `workers` jobs run at the same time, and they do run at the same time.
- `Submit` returns at once (it never waits for a worker). It fails, and changes nothing, for a
  job without `ID`, without `Run`, or with an `ID` already submitted.
- A job that returns `nil` is `Succeeded`; one that returns an error is `Failed` with that
  error in `Result.Err`. A job that **panics** is `Failed` too, with an error whose message
  contains `panic`: one bad job must not bring the scheduler down. `Attempts` is 1.
- `Wait` returns the results of every job submitted so far, keyed by ID, once all of them are
  finished. It can be called again after more `Submit` calls.
