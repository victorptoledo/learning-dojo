When more jobs are ready than there are free workers, the important ones go first. New
`Job` field:

```go
	Priority int // higher runs first; 0 by default
```

- Whenever a worker is free, it takes the ready job with the highest `Priority`; among equal
  priorities, the one submitted first.
- A job waiting for its dependencies is not ready: priority never lets a job jump over the
  jobs it waits for.
- A job already running is never stopped for a more important one.
