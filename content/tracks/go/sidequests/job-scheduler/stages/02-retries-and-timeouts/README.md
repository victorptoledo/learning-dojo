Flaky jobs get another chance; stuck jobs get a deadline. New `Job` fields:

```go
	Retries int           // how many more times a failed attempt is tried again
	Timeout time.Duration // the deadline of each attempt; 0 = none
```

- An attempt that returns an error (or panics) is tried again, up to `Retries` more times, at
  once. `Result.Attempts` counts the attempts made; `Err` is the error of the last one.
- With a `Timeout`, each attempt runs with a context that ends after it. A job that stops
  because of it returns the context's error, so `errors.Is(result.Err,
  context.DeadlineExceeded)` holds, and a timed-out attempt is retried like any failure.
- A worker stays busy while a job retries.
