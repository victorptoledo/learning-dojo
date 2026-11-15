# Overload: Timeouts, Retries, Back-Pressure and Load Shedding

Systems rarely die from the traffic they were designed for. They die when a dependency slows
down, requests pile up, clients retry, and the extra work arrives exactly when there is no
capacity left. This lesson is about keeping a service **useful at 2× its capacity** instead of
useless at 1.1×.

## How overload turns into an outage

```text
dependency slows (GC pause, hot shard)
  → requests take longer → more in flight (Little's law) → threads and connections run out
  → queues grow → requests wait longer than the client's timeout → work done for nobody
  → clients retry → even more load → the service never recovers on its own
```

Two ideas from this chain matter most. **Goodput** (useful, in-time responses) can fall to zero
while **throughput** stays at 100 %. And a system can enter a **metastable failure**: once the
backlog and the retries are there, removing the trigger is not enough to recover.

## Timeouts

Every remote call needs a timeout, chosen from data, not from a default:

- Measure the dependency's latency and set the timeout a little above a high percentile (say
  the p99.9), so false timeouts stay rare.
- Distinguish the **connect timeout** (short: a host that does not answer the handshake is
  gone) from the **request timeout**.
- Propagate a **deadline** down the call chain (gRPC does this): if the caller gives up in 300 ms,
  a server that is 280 ms in should not start a 200 ms query.
- A timeout without a retry policy only turns a slow failure into a fast one, which is still an
  improvement: it frees the thread.

## Retries, backoff and jitter

Retries fix transient failures and amplify persistent ones.

| Rule | Why |
| --- | --- |
| Retry only idempotent operations, or use idempotency keys | a retried payment must not charge twice |
| Retry only retryable errors (timeouts, 503, 429 with `Retry-After`) | a 400 will fail again |
| **Exponential backoff**: wait `base × 2^attempt`, capped | gives the dependency room to recover |
| **Jitter**: randomise the wait (`random(0, backoff)`, "full jitter") | stops clients from retrying in synchronised waves |
| Limit attempts (2–3) and use a **retry budget** (retries ≤ 10 % of requests) | caps the amplification |
| Retry at **one** layer only | 3 layers × 3 attempts = 27 calls to the bottom service |

**Circuit breakers** go one step further: after many failures, stop calling the dependency for a
while and fail fast (or serve a fallback), then let a few probe requests through.

## Back-pressure

Back-pressure means a component that is full **tells the producer to slow down** instead of
silently buffering. Examples:

- a **bounded queue** whose `offer` fails when full, so the caller can answer `429` or `503`;
- TCP's receive window, which stops a sender that outpaces the reader;
- reactive streams (`request(n)`) and Kafka consumers that only pull what they can process;
- a semaphore or a fixed pool that caps **concurrency** to a dependency (a bulkhead).

An **unbounded** queue is the opposite: it converts overload into latency and memory until the
process falls over, and every item in it is older than the last.

## Queues: level the load, do not hide it

A queue in front of workers absorbs bursts (**queue-based load leveling**): producers write at
the peak rate, workers consume at their steady rate. That works if the average arrival rate is
below the service rate; otherwise the backlog grows forever. Design rules from the Amazon
Builders' Library:

- **Bound** the queue and decide what happens when it is full.
- Track the **age** of the oldest message, not only the count; alert on age.
- **Drop work nobody waits for**: a request that has queued longer than the client's timeout is
  already a failure; serving it wastes capacity that fresh requests need.
- Under a large backlog, consider serving the **newest first** (LIFO): new requests still have a
  waiting client, old ones mostly do not.
- Separate queues (or quotas) per customer or priority so one noisy producer cannot starve the
  rest.

## Load shedding

When demand exceeds capacity, **reject early and cheaply** so the work you accept finishes in
time.

| Technique | How |
| --- | --- |
| Concurrency limit | reject when more than N requests are in flight (adaptive limits adjust N from latency) |
| Queue bound and deadline | reject when the queue is full; drop queued work past its deadline |
| Priority | shed best-effort traffic (prefetches, analytics, retries) before critical traffic (checkout, login) |
| Per-client quotas | rate limits and fair shares (token buckets from the Backend track) |
| Graceful degradation | serve a cached or simplified answer (no recommendations) instead of nothing |

Shedding must be cheap: rejecting a request should cost far less than serving it, or the
rejections themselves overload the server. Answer `503` (or `429` for a client over its quota)
with `Retry-After`.

| Priority class | Example | Shed |
| --- | --- | --- |
| Critical | payment, login, order placement | last |
| Default | product pages, search | when the queue is mostly full |
| Sheddable | prefetching, recommendations, batch jobs, retries | first |

## A checklist for a service you own

1. Timeouts on every outbound call, below the caller's own timeout.
2. Retries: idempotent only, capped, backoff with jitter, one layer, a budget.
3. Bounded queues and pools; reject when full.
4. Deadlines propagated; expired work dropped.
5. Priorities and quotas so the important traffic survives.
6. A load test that drives the service to 2× capacity and checks that goodput stays flat.

## Summary

- Overload spreads through waiting: more in flight, longer queues, timeouts and retries.
- Timeouts from measured latency; deadlines across the call chain.
- Retries need idempotency, backoff with jitter, attempt limits, a budget and one layer.
- Back-pressure: bounded queues and concurrency limits that push back instead of buffering.
- Shed load early and cheaply, by priority, and drop work whose client has given up.

## Further reading

- Amazon Builders' Library, "Timeouts, retries, and backoff with jitter": <https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/>
- Amazon Builders' Library, "Using load shedding to avoid overload": <https://aws.amazon.com/builders-library/using-load-shedding-to-avoid-overload/>
- Amazon Builders' Library, "Avoiding insurmountable queue backlogs": <https://aws.amazon.com/builders-library/avoiding-insurmountable-queue-backlogs/>
- Google SRE book, "Handling Overload": <https://sre.google/sre-book/handling-overload/> and "Addressing Cascading Failures": <https://sre.google/sre-book/addressing-cascading-failures/>
- Marc Brooker, "Metastability and Distributed Systems": <https://brooker.co.za/blog/2021/05/24/metastable.html>
- Azure Architecture Center, "Queue-Based Load Leveling": <https://learn.microsoft.com/en-us/azure/architecture/patterns/queue-based-load-leveling>
