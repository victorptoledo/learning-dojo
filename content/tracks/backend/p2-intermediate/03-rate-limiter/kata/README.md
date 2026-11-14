# Rate Limiter

A **token bucket** holds up to `capacity` tokens and **starts full**. It refills continuously:
`refillTokens` per `refillPeriod` (5 per second means one token every 200 ms, so after 500 ms you
have 2.5 more). Each request takes one token; with no token, the request is refused (an API answers
`429 Too Many Requests` with `Retry-After`). The capacity allows short bursts; the refill rate is
the sustained limit.

## `TokenBucket(capacity, refillTokens, refillPeriod, clock)`

- Rejects a capacity, refill or period that is not positive (`IllegalArgumentException`).
- `tryAcquire(n)`: takes `n` tokens if **all** of them are there, otherwise takes **nothing**.
- `available()`: whole tokens now (2.5 → 2). Never more than `capacity`, however long it waited.
- `retryAfter()`: how long until one token is available, `Duration.ZERO` if one already is. Exact:
  an empty 5-per-second bucket answers exactly `200ms`, and `50ms` after 150 ms more.
- Every method reads "now" from the `Clock`, and is **thread-safe**.

## `RateLimiter(capacity, refillTokens, refillPeriod, clock)`

- One bucket **per client** id, created on first use.
- `check(client)` returns a `Decision(allowed, remaining, retryAfter)`: `remaining` is what is left
  after this request, `retryAfter` is `ZERO` when allowed.
- 1000 concurrent checks on a bucket of 100 must allow **exactly** 100.

> Tip: doubles drift (`0.1 + 0.2`). Keep the arithmetic exact by counting in "nanoseconds of refill":
> one token = `refillPeriod.toNanos()` units, and every elapsed nanosecond adds `refillTokens` units.
> Then `available = units / periodNanos` and the wait is `ceil((periodNanos - units) / refillTokens)`
> nanoseconds. `synchronized` methods, `ConcurrentHashMap.computeIfAbsent` for the buckets, and
> `Duration.between(last, now)`.
