Build, then test, then deploy. New `Job` field:

```go
	After []string // the IDs of the jobs that must succeed first
```

- A job with `After` starts only once **every** job it names has `Succeeded`. While it waits
  it takes no worker.
- If one of them ends any other way, the job never runs: it is `Skipped`, with an `Err` that
  names the job it depended on. Jobs waiting on a skipped job are skipped too, and so on.
- `Submit` fails for a job that names itself or an ID that was not submitted before (so there
  can be no cycle).
