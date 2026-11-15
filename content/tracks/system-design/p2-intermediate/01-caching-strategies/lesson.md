# Caching Strategies (Redis) and Expiration Policies

A cache keeps a copy of data where it is cheaper or faster to read. It is the most effective
scaling tool there is and the most common source of subtle bugs: every cache is a **second
copy** of the truth, and the design question is always how stale that copy may be and what
happens when it is missing.

## Where caches live

| Layer | Example | Shared by | Typical lifetime |
| --- | --- | --- | --- |
| Browser / app | HTTP cache, local storage | one user | seconds to a year |
| CDN / edge | PoPs | everyone in a region | seconds to a year |
| Reverse proxy | NGINX, Varnish | everyone | seconds |
| In-process | Caffeine, a `HashMap` | one instance | seconds to minutes |
| Distributed | Redis, Memcached | all instances | seconds to hours |
| Database | buffer pool, query cache | the database | managed by the engine |

An in-process cache is the fastest (no network) but each instance has its own copy, so N
instances means N different staleness windows and N cold caches after a deploy. A distributed
cache is one copy for everyone, at the cost of a network hop (~0.5 ms) and a new dependency.

## The numbers that justify a cache

```text
effective latency = hit ratio × cache latency + (1 − hit ratio) × origin latency
90 % hits, 1 ms cache, 50 ms database → 0.9 × 1 + 0.1 × 50 = 5.9 ms
99 % hits                             → 0.99 × 1 + 0.01 × 50 = 1.5 ms
```

Going from 90 % to 99 % hits cuts the database load **ten times**. Measure the hit ratio per
key family, not globally: a 95 % average can hide a 20 % family that does all the damage.
Size the cache for the **working set** (the data read in a typical window), not for all data.

## Read and write patterns

| Pattern | Read | Write | Watch out for |
| --- | --- | --- | --- |
| Cache-aside (lazy loading) | app checks cache; on miss reads DB and fills cache | app writes DB, then **deletes** the key | the race below; cold start |
| Read-through | cache library loads from DB on miss | as cache-aside | the loader must be idempotent |
| Write-through | from cache | app writes cache, cache writes DB synchronously | every write pays both; cache full of unread data |
| Write-behind (write-back) | from cache | cache acknowledges, writes DB later in batches | data loss if the cache dies; ordering |
| Refresh-ahead | from cache | background refresh before expiry | wasted work on keys nobody reads |

**Delete, don't update**, on write: two concurrent writers that each `SET` the cache can leave
the older value cached forever, while a delete only causes one extra miss.

Even delete has a race in cache-aside:

```text
reader: miss → reads DB (old value) ............................. SET old value  ← stale!
writer:               updates DB → DELETE key
```

Mitigations: a **TTL** as the safety net (stale at most that long), Facebook's **leases** (the
cache hands a token to the first reader on a miss and rejects its `SET` if a delete happened
meanwhile), or invalidation from the database's change stream (CDC) instead of the application.

## Expiration and eviction

**Expiration** removes data that is too old (correctness); **eviction** removes data to make
room (capacity).

| Policy | Evicts | Good for |
| --- | --- | --- |
| TTL | entries older than their time to live | bounding staleness |
| LRU (least recently used) | the entry untouched for longest | most workloads, recency matters |
| LFU (least frequently used) | the entry read least often | stable popularity (a catalogue) |
| FIFO | the oldest inserted | simple, rarely best |
| Random | any entry | surprisingly good, no bookkeeping |
| W-TinyLFU (Caffeine) | admits by frequency, evicts by recency | resists scans and one-hit wonders |

Redis applies a `maxmemory-policy` such as `allkeys-lru`, `volatile-ttl` or `allkeys-lfu`, with
approximated (sampled) LRU and LFU. Add **jitter** to TTLs (300 s ± 10 %) so keys written
together do not all expire in the same second.

## Stampedes, hot keys and other failure modes

- **Cache stampede (dog-pile)**: a popular key expires and a thousand requests miss at once,
  all hitting the database. Fixes: **request coalescing / single flight** (one loader per key,
  the others wait for its result), **stale-while-revalidate** (serve the old value while one
  request refreshes), **probabilistic early refresh** (refresh a little before expiry with a
  probability that grows as expiry approaches).
- **Hot key**: one key gets so much traffic that its shard melts. Fixes: an in-process cache in
  front for a few seconds, or replicate the key under several names (`key#1..#8`) and read one
  at random.
- **Cold cache**: after a deploy or a cache failure the hit ratio is 0 and the database sees the
  full load. Warm up gradually, keep the database sized for a realistic miss rate, and shed load
  rather than fall over (next lesson).
- **Negative caching**: cache "not found" briefly too, or a stream of requests for missing ids
  goes straight to the database (or a Bloom filter in front).
- **Large values**: a 5 MB value blocks a single-threaded Redis for milliseconds; split or
  compress, and keep values small.

The Amazon Builders' Library warns that a cache often becomes an **undeclared dependency**: the
service was sized with the cache, so losing the cache is an outage. Design for it.

## Redis in production, briefly

| Topic | What to know |
| --- | --- |
| Data structures | strings, hashes, lists, sets, sorted sets (leaderboards, timelines), streams, HyperLogLog |
| Single-threaded commands | one slow command (`KEYS *`, a huge `SMEMBERS`) blocks everyone; use `SCAN` |
| Replication | asynchronous primary → replicas; a failover can lose recent writes |
| Redis Cluster | 16,384 hash slots spread over primaries; clients route by slot |
| Persistence | RDB snapshots and/or the AOF log; a cache may run with neither |
| Atomic updates | `INCR`, `SET key value NX PX 30000`, Lua scripts, `MULTI` |

## Summary

- Every cache is a second copy: decide the acceptable staleness and the behaviour on a miss.
- Hit ratio drives everything; 90 % → 99 % cuts origin load tenfold.
- Cache-aside with delete-on-write plus a TTL safety net is the default; know its race.
- Expiration bounds staleness, eviction bounds memory; add TTL jitter.
- Protect against stampedes (single flight, stale-while-revalidate), hot keys, cold caches and
  negative lookups, and treat the cache as a real dependency.

## Further reading

- Amazon Builders' Library, "Caching challenges and strategies": <https://aws.amazon.com/builders-library/caching-challenges-and-strategies/>
- Nishtala et al., "Scaling Memcache at Facebook" (leases, regions): <https://www.usenix.org/system/files/conference/nsdi13/nsdi13-final170_update.pdf>
- Azure Architecture Center, "Cache-Aside pattern": <https://learn.microsoft.com/en-us/azure/architecture/patterns/cache-aside>
- Redis documentation, "Key eviction": <https://redis.io/docs/latest/develop/reference/eviction/>
- RFC 9111, "HTTP Caching": <https://www.rfc-editor.org/rfc/rfc9111>
