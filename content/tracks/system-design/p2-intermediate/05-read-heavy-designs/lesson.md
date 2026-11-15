# Read-Heavy Designs: Fan-out, Denormalization and Read Models

Most products read far more than they write: feeds, catalogues, profiles, dashboards. The
general move is to **do work at write time so reads are cheap**: precompute, copy and
denormalise. This lesson walks through the patterns, using the classic example of a social
feed.

## The feed problem

A user follows 300 accounts and opens the app; the home timeline shows the newest posts of
those accounts. Two ways to build it:

| | Fan-out on read (pull) | Fan-out on write (push) |
| --- | --- | --- |
| On post | store the post once | also insert its id into every follower's **inbox** |
| On read | fetch recent posts of all 300 followees, merge, sort | read one precomputed inbox |
| Read cost | high: 300 lookups per open | low: one lookup |
| Write cost | low | proportional to the number of followers |
| Problem case | users who follow many accounts | accounts with millions of followers |

With a 100:1 read/write ratio, pushing wins for normal accounts. But a celebrity with 50 million
followers would cause 50 million inbox writes per post, minutes of work and a huge spike. The
answer used by large social networks is a **hybrid**:

```text
post by a normal account (followers < threshold)  → push the id into each follower's inbox
post by a celebrity                                → store it only in the celebrity's own list
read a timeline                                    → inbox ∪ recent posts of followed celebrities,
                                                     merged by time, filtered, top N
```

Details that matter in practice:

- Inboxes hold **ids**, not posts, and are **bounded** (the newest 800, say); older pages come
  from a slower path.
- Filter at read time: unfollows, blocks and deleted posts are applied when the timeline is
  read, instead of rewriting millions of inboxes.
- Fan-out runs **asynchronously** on a queue, so posting returns immediately; followers see the
  post a moment later (eventual consistency, and fine for a feed).
- Inactive users do not need their inbox maintained; rebuild it on their next login.

You implement this hybrid in the phase checkpoint.

## Denormalization

A normalised schema stores each fact once and joins at read time. At scale, joins across
partitions are expensive, so you **copy** the data next to where it is read:

| Normalised | Denormalised |
| --- | --- |
| `orders` joins `customers` for the name | the order row stores `customer_name` |
| count `likes` rows per post | `posts.like_count`, updated on each like |
| join three tables for a product page | one document with everything the page shows |

The price: every copy must be updated when the source changes, and copies drift if an update
is lost. Prefer copying **immutable** facts (the name at the time of the order is the right
value forever) and recomputing derived ones from an event log when they drift.

## Materialized views and CQRS

A **materialized view** is a query result stored and kept up to date: by the database
(PostgreSQL `REFRESH MATERIALIZED VIEW`, periodically) or by a consumer of change events. Taken
further, **CQRS** (command query responsibility segregation) separates the write model (validated
commands, normalised, transactional) from one or more **read models** shaped for each screen:

```text
commands → write database → change events (outbox / CDC) → projector → read model per query
                                                                        (search index, cache,
                                                                         document store, counts)
```

Benefits: each side scales and is optimised independently, and new read models can be built by
replaying events. Costs: read models lag behind writes (show "processing" or read your own
writes from the primary), and you operate more stores. Use CQRS for the few hot, complex read
paths, not as the default for every table.

## Counters and hot rows

`UPDATE posts SET likes = likes + 1` on a viral post makes one row the hottest lock in the
database. Options:

- **Sharded counters**: N rows per counter (`likes#0..#15`), increment a random one, sum on read
  (cache the sum).
- **Buffer and batch**: count in memory or in Redis (`INCR`) and flush to the database every few
  seconds.
- **Approximate**: HyperLogLog for unique viewers, where an error of 1 % is fine.

## Pagination for large reads

| Offset (`LIMIT 20 OFFSET 10000`) | Cursor / keyset (`WHERE (created_at, id) < (?, ?) LIMIT 20`) |
| --- | --- |
| the database still walks the 10,000 skipped rows | jumps straight to the position with the index |
| pages shift when new items arrive | stable while items are added |
| allows "go to page 500" | only next/previous, which is what feeds need |

Return an opaque cursor (the encoded last `(created_at, id)`) and keep a unique tie-breaker in
the sort so no item is skipped or repeated.

## Choosing the tools

| Need | Tool |
| --- | --- |
| The same answer for everyone | CDN, HTTP caching |
| A precomputed list per user | inbox in Redis sorted sets or a wide-column store |
| A screen that joins many entities | a denormalised document / read model |
| Full-text and faceted search | a search index fed by change events |
| Heavy aggregates | a materialized view or an analytics store |
| More read throughput on the same data | read replicas (next phase) |

## Summary

- Shift work from reads to writes when reads dominate: precompute, copy, denormalise.
- Feeds: push to inboxes for normal accounts, pull for celebrities, merge at read time.
- Filter unfollows and deletions at read time; bound inboxes; fan out asynchronously.
- Materialized views and CQRS read models trade freshness and operational cost for fast reads.
- Shard or batch hot counters; paginate with cursors, not offsets.

## Further reading

- Raffi Krikorian, "Timelines at Scale" (InfoQ talk on Twitter's fan-out): <https://www.infoq.com/presentations/Twitter-Timeline-Scalability/>
- Facebook engineering, "TAO: The power of the graph": <https://engineering.fb.com/2013/06/25/core-infra/tao-the-power-of-the-graph/>
- Azure Architecture Center, "CQRS pattern": <https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs> and "Materialized View pattern": <https://learn.microsoft.com/en-us/azure/architecture/patterns/materialized-view>
- Martin Kleppmann, *Designing Data-Intensive Applications*, chapter 1 (the timeline example) and chapter 11: <https://dataintensive.net/>
- Markus Winand, "Paging Through Results" (keyset pagination): <https://use-the-index-luke.com/>
