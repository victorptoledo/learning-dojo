One greedy client must not take the upstream down. New fields:

```go
	RatePerSecond float64          // tokens added per second to each client's bucket; 0 = no limit
	Burst         int              // the bucket's size, full at first
	Now           func() time.Time // the clock; nil means time.Now
```

- Each client (its IP from `r.RemoteAddr`, never from a header it could fake) has a token
  bucket. A request takes one token; with no token left, the answer is `429` as a problem
  document with a `Retry-After` header: the whole seconds until the next token, rounded up.
- A refused request never reaches the upstream.
- Buckets refill continuously (`RatePerSecond × elapsed seconds`, never above `Burst`).
- Many requests arrive at once: the buckets must be safe for concurrent use.

The tests drive the clock through `Now`, so no test waits for real seconds.
