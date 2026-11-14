# Scaling a Backend

A service that handles 100 requests per second rarely needs clever architecture. One that must
handle 100,000, or keep working when a data centre goes dark, does. Scaling is mostly about removing
the things that stop you from adding more machines: **state** in the wrong place, **a single
database** doing everything, and **coordination** between nodes.

## Vertical and horizontal

- **Vertical** scaling: a bigger machine. Simple, no code changes, and it has a ceiling (and a
  price curve that gets steep).
- **Horizontal** scaling: more machines behind a load balancer. No ceiling in principle, but only if
  any instance can serve any request.

Do the cheap things first: fix slow queries, add indexes and caches, raise the connection pool
sensibly. Many "scaling problems" are an N+1 query.

## Stateless services

An instance is **stateless** when it keeps nothing a later request needs: no sessions in memory, no
uploaded files on local disk, no in-memory counters that must be global. Then the load balancer can
send each request anywhere, instances can be added and removed freely, and a crash loses nothing.

Move state out:

| State | Where it goes |
| --- | --- |
| Sessions | a shared store (Redis) or self-contained tokens |
| Files | object storage (S3 and compatible) |
| Rate limits, locks, counters | Redis or the database |
| Scheduled jobs | one runner with a lock, or a job scheduler |

## Load balancing

A load balancer spreads requests across healthy instances.

- **Algorithms**: round robin, least connections, or hashing a key (consistent hashing keeps most
  keys on the same instance when the pool changes).
- **Health checks**: the balancer sends traffic only to instances whose readiness check passes.
- **Layer 4** (TCP) balancers are fast and simple; **layer 7** (HTTP) balancers can route by path or
  header, terminate TLS and retry.
- Avoid **sticky sessions** unless you must: they bring back state and uneven load.

## Scaling the database

The database is usually the hardest part, because it holds the state you moved there.

1. **Optimise**: indexes, query fixes, connection pooling, caching hot reads.
2. **Read replicas**: copies of the primary that serve reads. Replication is usually
   **asynchronous**, so a replica can be a little behind (**replication lag**): a user who just
   saved a change may not see it on a replica. Read your own writes from the primary, or wait for the
   replica to catch up.
3. **Partitioning (sharding)**: split the data across several databases by a **shard key**
   (customer id, tenant id). Each shard holds part of the data and takes part of the load.

Sharding is a big step. Pick the key so that most queries touch **one shard** and load spreads
evenly (a "celebrity" key that gets most of the traffic becomes a hot shard). Cross-shard queries,
joins and transactions become expensive or impossible, and rebalancing shards is real work.

## CAP and PACELC

When a network **partition** separates nodes of a distributed system, the CAP theorem says it must
choose between:

- **Consistency**: every read sees the latest write (some requests fail or wait), or
- **Availability**: every request gets an answer (some answers may be stale).

Partitions are not optional in real networks, so the choice is really C or A during one. **PACELC**
adds the everyday trade-off: **E**lse, when there is no partition, choose between **L**atency and
**C**onsistency (synchronous replication is consistent but slower).

| System style | During a partition | Normally |
| --- | --- | --- |
| A single relational primary | consistent (writes stop if it is unreachable) | consistent |
| Dynamo-style stores (Cassandra, DynamoDB) | available, possibly stale | low latency, tunable |

## Consistency models

- **Strong**: reads always see the latest write.
- **Read-your-writes**: a user always sees their own changes (often all a UI needs).
- **Eventual**: replicas converge if writes stop; reads may be stale for a while.

Choose per use case: a bank balance after a transfer needs strong consistency for that user; a "like"
counter can be eventual.

## Back-of-the-envelope

Before designing, estimate. 10 million daily users, each making 20 requests a day, is about 200 million
requests a day, roughly **2,300 per second on average**, and peaks are often 3 to 5 times higher. If
each request reads 2 KB, that is a few MB per second. Numbers like these tell you whether one database
is plenty (often it is) or whether you need replicas and caches.

## Summary

- Optimise first; scale vertically while it is cheap; scale horizontally with stateless instances.
- Move sessions, files, counters and locks out of the instances.
- Load balancers route to healthy instances; avoid sticky sessions.
- Scale databases with indexes and caches, then read replicas (mind replication lag), then sharding
  by a well-chosen key.
- CAP: during a partition choose consistency or availability; PACELC: otherwise latency or
  consistency. Pick the consistency model per use case, and estimate the load before designing.
