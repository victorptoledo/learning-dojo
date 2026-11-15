# Capacity Estimator

Write the arithmetic every design review starts with. All methods are `static`, and every
result that is a count is **rounded up**: you cannot buy 6.2 servers.

## `Estimates`

| Method | Returns |
| --- | --- |
| `requestsPerSecond(dailyUsers, requestsPerUserPerDay)` | average requests per second over a 86,400-second day, rounded up |
| `peak(average, factor)` | `average × factor`, rounded up (`factor` at least 1) |
| `storageBytes(itemsPerDay, bytesPerItem, days, replicas)` | total bytes stored, every copy counted |
| `serversNeeded(peakRps, rpsPerServer, maxUtilizationPercent)` | servers so that none runs above the utilization at peak, **plus one spare** (N+1) |
| `inFlight(rps, latency)` | Little's law: requests in flight = throughput × latency, rounded up |

- Negative counts, a factor below 1, a server capacity below 1 or a utilization outside
  1–100 throw `IllegalArgumentException`.
- `storageBytes` must not overflow silently: use `Math.multiplyExact` (it throws
  `ArithmeticException`).

## `Availability`

| Method | Returns |
| --- | --- |
| `serial(double... parts)` | the availability when **all** parts are needed (multiply) |
| `parallel(each, copies)` | the availability when **any one** copy is enough: `1 − (1 − each)^copies` |
| `downtimePerYear(availability)` | `Duration` of downtime in a 365-day year, rounded to whole seconds |

Availabilities are fractions between 0 and 1 (`0.999`, not `99.9`); anything else, no parts at
all, or fewer than one copy throws `IllegalArgumentException`.

Examples: `requestsPerSecond(10_000_000, 20)` is `2315`; `serversNeeded(7000, 1000, 70)` is
`11` (10 to stay under 70 %, plus a spare); `downtimePerYear(0.999)` is 8 h 45 min 36 s.

> Tip: `Math.ceilDiv(a, b)` rounds an integer division up. For `inFlight`, work in
> nanoseconds: `ceilDiv(rps * latency.toNanos(), 1_000_000_000)`.
