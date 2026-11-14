# Caching and Queues

Two ideas take load off the request path. A **cache** remembers answers so the expensive work is not
repeated. A **queue** takes slow work out of the request so the user does not wait for it. Both are
simple to add and easy to get subtly wrong.

## Where caches live

| Layer | Example | Good for |
| --- | --- | --- |
| Client / CDN | `Cache-Control`, ETags | public, cacheable responses: images, catalogue pages |
| In-process | Caffeine | hot data per instance, microsecond access, no network |
| Shared | Redis, Memcached | data shared by all instances, sessions, rate limit counters |

In-process caches are fastest but each instance has its own copy (and its own staleness). A shared
cache costs a network round trip but is consistent across instances.

## Patterns

**Cache-aside** (lazy loading), the most common:

```text
read:  value = cache.get(key)
       if missing: value = db.load(key); cache.set(key, value, ttl)
write: db.save(value); cache.delete(key)          ← delete, do not update
```

- Deleting on write (instead of writing the new value into the cache) avoids a race where an older
  value is written into the cache after a newer one.
- **Write-through**: every write goes to the cache and the database together; reads always hit.
- **Write-behind**: writes go to the cache and are flushed to the database later; fast, but data
  can be lost on a crash.

## Expiry and invalidation

> There are only two hard things in computer science: cache invalidation and naming things.

- Every entry gets a **TTL**: it bounds how stale data can be even if an invalidation is missed.
- Invalidate on write for data that must be fresh; rely on TTL alone for data where a little
  staleness is fine (a product's review count).
- Decide the staleness **per use case**: prices in a checkout must be fresh, a "popular items"
  list can be minutes old.
- Bound the size (`maximumSize` in Caffeine, `maxmemory` + eviction policy in Redis).

## Stampedes

A popular key expires; a thousand requests miss at the same moment and all hit the database.
Defences:

- **Single flight**: only one request recomputes; the others wait for its result
  (Caffeine's `get(key, loader)` does this per instance).
- **Jittered TTLs**: `ttl = base + random(0..10%)` so keys do not all expire together.
- **Refresh ahead**: refresh hot keys in the background before they expire.
- **Serve stale** briefly while one request recomputes.

## Queues: move slow work out of the request

Sending email, resizing images, generating reports, calling slow partners: none of these needs to
finish before the user gets an answer.

```text
POST /reports   →  validate, store a job, enqueue it  →  202 Accepted
                                                        Location: /reports/jobs/91
worker          ←  takes the job from the queue, does the work, stores the result
GET /reports/jobs/91  →  {"status": "DONE", "url": "..."}
```

`202 Accepted` says "received, not done yet"; the `Location` points to a status resource the client
can poll (or you notify it with a webhook, SSE or email).

What a queue gives you:

- **Smoothing**: bursts wait in the queue instead of overwhelming the workers.
- **Decoupling**: the API keeps working when the worker or the email provider is down.
- **Scaling**: add workers to drain the queue faster.

What it costs: more moving parts, eventual results, and delivery semantics you must handle
(messages can arrive twice or late; the next lesson).

## Brokers in one paragraph each

- **RabbitMQ**: a classic message broker. Messages are routed to queues and **removed when
  acknowledged**. Great for work queues and routing.
- **Kafka**: a distributed **log**. Messages are appended to partitions and **kept** for a retention
  period; consumers track their own position (offset) and can replay. Great for event streams, high
  throughput and many independent consumers.
- **Cloud queues** (SQS, Pub/Sub, Service Bus): managed, simple, pay per use.
- **A database table** used as a queue is fine for low volume and gives you transactions for free.

## Summary

- Cache at the right layer: HTTP/CDN, in-process (Caffeine) or shared (Redis).
- Cache-aside with delete-on-write, a TTL on every entry, a size bound, and a staleness decision
  per use case.
- Prevent stampedes with single flight, jitter, refresh-ahead or serving stale.
- Move slow work to a queue and answer `202 Accepted` with a status resource.
- RabbitMQ routes and deletes acknowledged messages; Kafka keeps a replayable log.
