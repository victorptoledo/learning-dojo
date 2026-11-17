Deploys restart processes; a scheduler must stop without losing track of anything.

```go
var ErrClosed = errors.New("scheduler: closed")

func (s *Scheduler) Shutdown(ctx context.Context) error
```

- From the moment `Shutdown` is called, `Submit` fails with an error that `errors.Is`
  `ErrClosed`.
- Jobs that have not started (waiting for a worker or for their dependencies) never will:
  they are `Canceled`.
- Running jobs are allowed to finish, and keep their result. If `ctx` ends first, the
  scheduler cancels the context of every running job, waits for them to return, marks the ones
  that stopped because of it `Canceled` (no retries any more), and `Shutdown` returns
  `ctx.Err()`. Otherwise it returns `nil` once everything is finished.
- `Wait` still works afterwards and reports every job.
