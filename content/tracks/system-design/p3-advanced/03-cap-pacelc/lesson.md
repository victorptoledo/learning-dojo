# CAP, PACELC, Consistency Models and Quorums

Once data lives on several machines connected by a network that can delay or drop messages,
"what does a read return?" stops having an obvious answer. This lesson gives you the vocabulary
to state precisely what a system promises, and the quorum arithmetic behind many of those
promises.

## Consistency models, from strong to weak

| Model | Promise | Cost |
| --- | --- | --- |
| **Linearizable** (strong) | the system behaves like one copy; once a write completes, every later read sees it | coordination on every operation; unavailable when a minority is cut off |
| **Sequential** | all clients see operations in the same order, consistent with each client's own order | still needs a global order |
| **Causal** | operations that depend on each other are seen in that order by everyone; unrelated ones may differ | track dependencies (version vectors) |
| **Read-your-writes, monotonic reads** | per-session guarantees: you see your own writes, time does not go backwards | session stickiness or tokens |
| **Eventual** | if writes stop, replicas converge | cheap, available; reads may be stale or out of order |

Linearizability is what people usually mean by "consistent", and it is what you need for a
lock, a leader election, a unique username or an account balance check. Most other data
(a like count, a profile photo, a product description) is fine with weaker models.

## CAP, stated carefully

When a network **partition** separates replicas, a system that keeps accepting requests on both
sides cannot also keep them linearizable. During the partition you choose:

- **CP**: refuse (or block) some requests to stay consistent, e.g. the minority side of a
  ZooKeeper, etcd or Spanner group stops accepting writes.
- **AP**: keep answering on both sides and reconcile afterwards, e.g. Cassandra or DynamoDB with
  weak settings, DNS, a shopping cart.

Common misreadings: CAP is not "pick two of three" in normal operation (without a partition you
can have both); partitions are not optional (networks do fail); and the choice can be made per
operation, not per product.

## PACELC: the everyday trade-off

Partitions are rare; latency is constant. **PACELC** extends CAP: if there is a **P**artition,
choose **A** or **C**; **E**lse (normal operation), choose **L**atency or **C**onsistency.
Waiting for remote replicas to confirm costs a round trip on every write, possibly across zones
or regions.

| System (typical configuration) | Partition | Else |
| --- | --- | --- |
| Cassandra, DynamoDB (eventual reads) | A | L |
| MongoDB (majority writes, primary reads) | C | C |
| Spanner, CockroachDB | C | C |
| PostgreSQL primary + async replicas | C on the primary | L for replica reads |

## Quorums

Leaderless (Dynamo-style) stores write each key to `N` replicas and use two numbers per request:

- `W`: how many replicas must acknowledge a write;
- `R`: how many replicas must answer a read; the newest version among them wins.

If **`W + R > N`**, every read set overlaps every write set in at least one replica, so a read
sees the latest acknowledged write (with caveats below).

| N, W, R | Overlap? | Tolerates | Typical use |
| --- | --- | --- | --- |
| 3, 2, 2 | yes | one replica down for reads and writes | the balanced default |
| 3, 3, 1 | yes | fast reads, no replica down for writes | read-heavy, rare writes |
| 3, 1, 3 | yes | fast writes, all replicas needed for reads | write-heavy logs |
| 3, 1, 1 | **no** | anything; reads may be stale | caches, metrics |

Caveats that make quorums weaker than linearizable: concurrent writes need a rule to pick a
winner (last write wins by timestamp drops one; **version vectors** detect true conflicts and
keep both "siblings" for the application to merge); a write that reached fewer than `W` replicas
fails but is not rolled back, so it may surface later; and **sloppy quorums** (writing to
whatever nodes are reachable and handing the data back later, "hinted handoff") favour
availability over the overlap guarantee.

## Repairing divergence

| Mechanism | When it runs | What it does |
| --- | --- | --- |
| **Read repair** | during a read | replicas that returned an older version are sent the newest |
| **Hinted handoff** | when a replica comes back | a stand-in node forwards the writes it held for it |
| **Anti-entropy** | in the background | replicas compare **Merkle trees** (hashes of key ranges) and sync only the ranges that differ |

You implement a quorum store with read repair in the next kata.

## Choosing per operation

| Operation | Needs | Choice |
| --- | --- | --- |
| Claiming a username, a seat, a coupon | exactly one winner | linearizable: a single leader or consensus, a conditional write |
| Showing a feed or a product page | fast and available | eventual, cached |
| Viewing your own settings after saving | your own writes | read-your-writes session guarantee |
| Displaying chat messages | replies after questions | causal (same partition per conversation) |
| Counting views | approximate is fine | eventual, CRDT counter |

## Summary

- Name the consistency model you need; linearizability only where correctness demands it.
- CAP: during a partition, choose consistency or availability, per operation.
- PACELC: in normal operation, every consistency guarantee costs latency.
- Quorums: `W + R > N` makes reads overlap the latest write; tune W and R for the workload.
- Read repair, hinted handoff and Merkle-tree anti-entropy bring replicas back together.

## Further reading

- Daniel Abadi, "Consistency Tradeoffs in Modern Distributed Database System Design" (PACELC): <https://www.cs.umd.edu/~abadi/papers/abadi-pacelc.pdf>
- Eric Brewer, "CAP Twelve Years Later: How the Rules Have Changed": <https://www.infoq.com/articles/cap-twelve-years-later-how-the-rules-have-changed/>
- Jepsen, "Consistency Models" (a map of the models): <https://jepsen.io/consistency>
- Cassandra documentation, "Dynamo" architecture (quorums, hinted handoff, Merkle trees): <https://cassandra.apache.org/doc/latest/cassandra/architecture/dynamo.html>
- Martin Fowler's site, Unmesh Joshi, "Quorum": <https://martinfowler.com/articles/patterns-of-distributed-systems/quorum.html>
