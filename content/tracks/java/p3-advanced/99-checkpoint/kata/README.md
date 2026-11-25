# Checkpoint 3: Freight Quotes

A shop asks several carriers for a price before it ships an order. Each carrier is a slow
remote API, some fail, some hang, and each one allows only a few calls at a time. Build the
`FreightQuoter` that asks them all **at the same time**, keeps to their limits, and picks the
best offer.

Already written: `Shipment`, `Quote`, `Carrier` (blocking, may throw, may return `null`), the
sealed `QuoteResult` (`Quoted`, `Failed`, `TimedOut`), `QuoteBoard` and `Stats`. You write
`Choice` and `FreightQuoter`.

## 1. `Choice`: a strategy as a lambda

`Choice.pick(quotes)` returns the best quote, or `Optional.empty()` when there is none. Write
the three factories, each returning a lambda (no new classes):

| Factory | Picks | Ties go to |
| --- | --- | --- |
| `cheapest()` | the lowest `priceCents` | fewer `days`, then the carrier name (A before B) |
| `fastest()` | the fewest `days` | the lower price, then the carrier name |
| `cheapestWithin(maxDays)` | the cheapest of the quotes with `days <= maxDays` | as `cheapest()` |

## 2. `FreightQuoter`

`new FreightQuoter(carriers, maxCallsPerCarrier, timeout)` refuses an empty carrier list, a
limit below 1 and a timeout that is zero or negative (`IllegalArgumentException`).

**`quote(shipment, choice)`** asks every carrier, each on **its own virtual thread**, and
returns a `QuoteBoard`:

- `results` has **one result per carrier, in the order of the carriers**, whatever order
  they answer in.
- The `timeout` is **one deadline for the whole quote**, counted from when `quote` starts. A
  carrier that has not answered by then is `TimedOut(name)`, and its call is **cancelled**
  (interrupted), so `quote` returns right away instead of waiting for it.
- A carrier that throws is `Failed(name, message)`, with the exception's class simple name
  when the message is `null`. A carrier that returns `null` is `Failed(name, "no quote")`.
- `best` is `choice.pick(...)` over the `Quoted` results only.

**At most `maxCallsPerCarrier` calls run at once on each carrier,** counted over **every**
`quote` running at that moment on this quoter. Virtual threads are cheap, so do not pool
them: limit the scarce thing, the carrier, with one `Semaphore` per carrier. A permit must
come back after a success, a failure **and** a cancelled call. Waiting for a permit counts
against the deadline.

**`ORDER_ID`** is a `ScopedValue<String>`. While a carrier runs, `ORDER_ID.get()` must return
the order id of the shipment it is quoting (the carriers log it). It must not stay bound in
the caller once `quote` returns. Careful: a binding is visible on **the thread that made it**;
the threads of an ordinary executor do not inherit it.

**`quoteAll(shipments, choice)`** quotes every shipment **at the same time** (one virtual
thread each) and returns one board per shipment, in order.

**`stats()`** returns the calls actually made to each carrier (a call that waited for a permit
and timed out before it got one was never made), the number of `Failed` results and the number
of `TimedOut` results since the quoter was built. Thousands of threads update these at once:
none may be lost.

**`summary(board)`** (static) uses a `switch` over the sealed `QuoteResult`:

```
2 quoted, 1 failed, 1 timed out; best: Rapido at 1250 cents in 2 days
0 quoted, 0 failed, 1 timed out; no quote
```

## What the grading tests check

The fake carriers sleep, throw, hang, count how many calls run at once, check that they run on
a virtual thread with `ORDER_ID` bound, and note when they are interrupted. The tests measure
time: six carriers of 400 ms must answer in well under 1.2 s, and 40 shipments on carriers that
allow 4 calls of 50 ms at once must use all 4 (about 500 ms) but never 5.

> [!TIP]
> Use the JDK only: `Executors.newVirtualThreadPerTaskExecutor()` in try-with-resources,
> `Future.get(left, TimeUnit.NANOSECONDS)`, `future.cancel(true)`, `Semaphore` with
> `acquire` and `release` in `finally`, `ScopedValue.where(...).call(...)`, `LongAdder` and
> `ConcurrentHashMap`. Run the visible tests with `mvn test`.
