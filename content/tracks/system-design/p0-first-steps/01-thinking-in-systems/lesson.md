# Thinking in Systems: Requirements, Estimates and Availability

System design is the work of choosing components and the ways they talk, so that a product
keeps its promises (fast enough, correct enough, up enough, cheap enough) as the load and the
failures grow. There is rarely one right answer: a design is a set of **trade-offs** you can
explain and defend with numbers.

## Why it is a discipline of its own

One server with a database answers a surprising amount of traffic. You need system design
when one of these stops being true:

| Pressure | What breaks first | Typical answer |
| --- | --- | --- |
| More reads | the database CPU, the disk | caches, read replicas, a CDN |
| More writes | one primary database | partitioning (sharding), queues to smooth bursts |
| More data | one disk, one index | partitioning, cold storage, compaction |
| Users far away | the speed of light | CDNs, edge caching, regional deployments |
| Failures | one machine, one zone, one region | redundancy, failover, isolation |
| More teams | one codebase and one deploy | service boundaries, contracts, async messaging |

Each answer adds moving parts, and every moving part can fail. The art is to add only what the
requirements justify.

## Start with requirements, not boxes

Split them in two before drawing anything:

- **Functional**: what the system does. "Users post photos; followers see them in a feed."
- **Non-functional** (the "-ilities"): how well it must do it.

| Non-functional requirement | A question that turns it into a number |
| --- | --- |
| Latency | p99 of the feed request under 300 ms? |
| Throughput | how many requests per second at peak? |
| Availability | 99.9 % (8.8 hours down a year) or 99.99 % (53 minutes)? |
| Durability | may we ever lose an accepted write? |
| Consistency | must a user see their own post immediately? everyone else? |
| Cost | what does one request, or one stored GB, cost us? |

Ask about the **read/write ratio**, the **size of an item**, the **growth** per year and the
**access pattern** (recent items hot, old items cold). These four facts drive most decisions.

## Latency numbers worth knowing

Orders of magnitude, not benchmarks; they tell you which designs are even possible:

| Operation | Rough time |
| --- | --- |
| Main memory reference | 100 ns |
| Read 1 MB sequentially from memory | 3–10 µs |
| Round trip inside one datacenter | 0.5 ms |
| Read 1 MB from an SSD | ~0.2–1 ms |
| Random disk seek (spinning disk) | 10 ms |
| Round trip Europe ↔ US East | 70–90 ms |
| Round trip Europe ↔ Australia | 250–300 ms |

Consequences: a request that calls the database 50 times in sequence spends 25 ms only on
network round trips; a user in Sydney waits a quarter of a second for every round trip to
Frankfurt, so TLS handshakes and redirects hurt more than your code.

## Back-of-the-envelope estimates

Round aggressively; the goal is the **order of magnitude**. A day has 86,400 seconds, call it
10⁵.

```text
Photo app: 10 M daily active users, 20 feed reads and 0.5 uploads per user per day
Reads:   10 M × 20 / 86,400 ≈ 2,300 requests/s on average
Peak:    ×3 at the evening peak ≈ 7,000 requests/s
Writes:  10 M × 0.5 / 86,400 ≈ 60 uploads/s   → read/write ratio ≈ 40:1
Storage: 5 M photos/day × 2 MB = 10 TB/day ≈ 3.6 PB/year (before replication!)
Servers: one app server handles ~1,000 req/s at 60 % CPU → 7 for the peak, plus spares
```

The estimate already tells you the shape: reads dominate (caching and a CDN for the photos),
photos go to object storage, not to the database, and the metadata (a few hundred bytes per
photo) fits comfortably in a partitioned database.

**Little's law** links the three numbers you will meet again and again:
`concurrency = throughput × latency`. 7,000 req/s at 200 ms each means about 1,400 requests in
flight at any moment, which sizes thread pools and connection pools.

## Availability in numbers

Availability is the fraction of time (or of requests) that succeed.

| Availability | Down per year | Down per month |
| --- | --- | --- |
| 99 % ("two nines") | 3.65 days | 7.3 hours |
| 99.9 % | 8.8 hours | 43.8 minutes |
| 99.95 % | 4.4 hours | 21.9 minutes |
| 99.99 % | 52.6 minutes | 4.4 minutes |
| 99.999 % | 5.3 minutes | 26 seconds |

Components combine in two ways:

- **In series** (the request needs all of them): multiply. A load balancer (99.99 %), an app
  (99.9 %) and a database (99.95 %) give `0.9999 × 0.999 × 0.9995 ≈ 99.84 %`, worse than each.
- **In parallel** (any one copy is enough): `1 − (1 − a)ⁿ`. Two independent app servers at
  99 % give `1 − 0.01² = 99.99 %`.

The catch is the word **independent**: two servers in the same rack, on the same deploy, with
the same bug, fail together. Much of this track is about making failures independent.

## A process for any design question

1. **Clarify** the functional requirements and the scale. Write the numbers down.
2. **Estimate** traffic, storage and bandwidth; spot the dominant load.
3. **Sketch the API and the data model**: the main calls and the main entities.
4. **Draw the high-level design**: clients, edge, services, storage, async parts.
5. **Deep-dive** on the hardest part (the hot path, the biggest table, the failure that hurts).
6. **Discuss trade-offs and failures**: what breaks at 10× the load? when a zone dies?

A good design states what it gives up. "Feeds may be up to 5 seconds stale, in exchange for
serving them from a cache" is a design; "use Kafka and Redis" is a shopping list.

## The building blocks of this track

| Phase | Blocks | Kata |
| --- | --- | --- |
| P1 | DNS, CDN, load balancers, consistent hashing, storage engines | hash ring, Bloom filter |
| P2 | caching, timeouts and retries, back-pressure, fan-out | LRU + TTL cache, load-shedding queue |
| P3 | replication, sharding, consistency, quorums, messaging | partition rebalancer, quorum store |
| P4 | consensus, leases, cells, multi-region, full designs | fenced lease |

The katas are Java with JUnit, like the Modern Java track: each one builds a block you would
otherwise only draw as a box, with a clock you control so the tests are exact.

## Summary

- Design is trade-offs under requirements; write the non-functional ones as numbers.
- Know the latency orders of magnitude and estimate traffic, storage and servers quickly.
- Little's law: in-flight work = throughput × latency.
- Series availability multiplies down; parallel copies multiply the unavailability down, if
  their failures are independent.
- Clarify, estimate, API and data, high level, deep dive, trade-offs.

## Further reading

- roadmap.sh, System Design roadmap: <https://roadmap.sh/system-design>
- The System Design Primer: <https://github.com/donnemartin/system-design-primer>
- Google SRE book, "Embracing Risk": <https://sre.google/sre-book/embracing-risk/> and the
  availability table: <https://sre.google/sre-book/availability-table/>
- AWS Well-Architected Framework: <https://docs.aws.amazon.com/wellarchitected/latest/framework/welcome.html>
- Latency numbers, interactive: <https://colin-scott.github.io/personal_website/research/interactive_latency.html>
- Martin Kleppmann, *Designing Data-Intensive Applications*: <https://dataintensive.net/>
