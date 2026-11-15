# Fenced Lease

Two classes: `LeaseManager` (the lock service) and `FencedStore` (the resource it protects).
`Lease(owner, token, expiresAt)` is given.

## `LeaseManager(Duration leaseDuration, Clock clock)`

A zero or negative duration throws `IllegalArgumentException`. A lease is **expired from its
`expiresAt` on** (at exactly `expiresAt` it is gone).

| Method | Behaviour |
| --- | --- |
| `tryAcquire(owner)` | if there is no live lease, a new `Lease(owner, token, now + duration)` with the **next token** (1, 2, 3, ...); if any live lease exists, even the caller's own, `Optional.empty()` |
| `renew(lease)` | if `lease` is the live lease (same token, not expired, not released), a lease with the **same token** and `expiresAt = now + duration`; otherwise empty |
| `release(lease)` | if `lease` is the live lease, ends it and returns `true` (someone else may acquire at once); otherwise `false` and nothing changes |
| `current()` | the live lease, if any |
| `isSafeToAct(lease, margin)` | `true` only if `lease` is the live lease **and** `now` is before `expiresAt − margin` |

Tokens never repeat and never go down, whatever happens. Every method is **thread-safe**:
16 threads calling `tryAcquire` at once give exactly one lease.

## `FencedStore`

| Method | Behaviour |
| --- | --- |
| `write(key, value, token)` | if `token` is lower than the highest token seen, throw `StaleTokenException` and change nothing; otherwise store and remember the token |
| `read(key)` | the stored value |
| `highestToken()` | the highest token seen (0 before any write) |

The same token may write many times (the holder does many writes during its lease).

## The accident the tests replay

```text
A acquires token 1 ... A pauses for 15 s (the lease lasts 10 s)
B acquires token 2 and writes "B"
A wakes up and writes "A" with token 1  → StaleTokenException; the store still says "B"
```

> Tip: keep the live lease in a field (set it to `null` on release) and a `long lastToken`.
> "Is this the live lease?" means: there is one, it has the same token, and `now` is before its
> `expiresAt`. Make the methods `synchronized`.
