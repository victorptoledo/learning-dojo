# Replication, Horizontal Sharding and Partitioning

One database server eventually runs out of something: read capacity, write capacity, disk, or
the tolerance for losing it. **Replication** keeps copies of the same data on several machines
(for availability and read scale); **partitioning** (sharding) splits the data so each machine
holds a part (for write scale and size). Real systems do both: every partition is replicated.

## Replication topologies

| Topology | Writes go to | Strength | Hard part |
| --- | --- | --- | --- |
| Single leader (primary/replicas) | one leader | simple, no write conflicts | the leader is a bottleneck; failover |
| Multi-leader | a leader per region or datacenter | local writes in every region | conflicting concurrent writes |
| Leaderless (Dynamo style) | any replicas, with quorums | no failover needed, tolerant of slow nodes | conflict resolution, read repair (next lessons) |

### Synchronous or asynchronous

| | Synchronous | Asynchronous | Semi-synchronous |
| --- | --- | --- | --- |
| Leader acknowledges after | the replica has the write | its own disk only | at least one replica has it |
| Losing the leader loses | nothing acknowledged | the writes not yet shipped | nothing, if that replica takes over |
| Latency and availability | every write waits; a slow replica blocks writes | fast; replicas can lag | a compromise used by most setups |

PostgreSQL streaming replication, MySQL replication and Redis are asynchronous by default, with
synchronous options. Managed databases often replicate synchronously across zones (Aurora
writes to 4 of 6 storage copies in 3 zones).

## Replication lag and what users see

Reading from asynchronous replicas scales reads, but a replica may be seconds behind. Three
anomalies, and their fixes:

| Anomaly | Example | Guarantee that fixes it | How |
| --- | --- | --- | --- |
| Not seeing your own write | edit your profile, reload, the old one shows | **read-your-writes** | read from the leader for a while after writing, or wait until the replica reaches your write's position |
| Time going backwards | refresh twice, a comment appears then disappears | **monotonic reads** | pin each user to one replica |
| Effect before cause | the answer appears before the question | **consistent prefix** | keep causally related writes in the same partition |

## Failover

When the leader dies, a replica must be promoted. Every step is a trap:

1. **Detecting** the failure: a timeout too short fails over on a GC pause; too long means
   minutes of unavailability.
2. **Choosing** the new leader: the most up-to-date replica; with async replication, the newest
   writes on the old leader may be **lost**.
3. **Fencing** the old leader: if it comes back believing it still leads, two leaders accept
   writes (**split brain**). Phase 4 covers leases and fencing tokens.
4. **Reconfiguring** clients to the new leader (DNS, a proxy, or a service registry).

Tools such as Patroni (PostgreSQL), orchestrator (MySQL) and managed services automate this using
a consensus store (etcd, ZooKeeper) to agree on who leads.

## Multi-leader and conflicts

Two regions accept writes; the same row is changed in both before they sync. Options:
**last write wins** (by timestamp: simple, silently drops a write), **merge** application
specific (add both items to the cart), **CRDTs** (data types whose concurrent updates always
merge, such as counters and sets), or **avoid** the conflict by routing all writes of one
record to its "home" region. Collaborative editors and offline-first apps live here.

## Partitioning

| Strategy | Key → partition | Good | Bad |
| --- | --- | --- | --- |
| Key range | `A–F → p1, G–M → p2, ...` | range scans by key | hot spots (everyone writes "today") |
| Hash of key | `hash(key) mod P` | even spread | no range scans across keys |
| Consistent hashing | ring of nodes | few keys move on change | uneven without virtual nodes |
| Fixed partitions | `hash(key) mod 1024` partitions, assigned to nodes | rebalancing moves whole partitions | the number of partitions is fixed up front |
| Directory / lookup | a table maps each tenant to a shard | full control (move a big tenant alone) | the directory is a critical dependency |

The **partition key** is the most important decision: it must spread load evenly *and* keep the
data of a typical request together. `user_id` is common for user data; for time series, combine
the source and a time bucket (`sensor_id + day`) so no partition holds "now" for everyone.

### Hot spots and skew

Even a good hash cannot split one hot key (a celebrity's profile, a flash-sale product).
Mitigations: add a random suffix to the key and scatter writes (read all suffixes), cache the
hot key in front, or give hot tenants their own shard.

### Rebalancing

Data must move when nodes are added or removed. The approach most databases use (Cassandra,
Elasticsearch, Kafka, Riak, Couchbase): create **many more partitions than nodes** (say 256 for
10 nodes) and move **whole partitions**. A new node steals a few partitions from each existing
node; nothing else moves, and moving is copying files. You build this rebalancer in the next
kata. Other approaches split partitions dynamically when they grow (HBase, MongoDB, DynamoDB),
which suits key-range partitioning.

### Routing requests

Who knows which node holds a key? Either the **client** (Redis Cluster clients, the Cassandra
driver), a **routing tier** (a proxy such as Vitess's VTGate or mongos), or **any node**, which
forwards. The assignment itself is usually stored in a consensus store (ZooKeeper, etcd) or
gossiped between nodes.

### What gets harder

- **Cross-partition queries** scatter to every shard and gather: avoid them on hot paths.
- **Secondary indexes** are local (scatter on read) or global (remote write on update).
- **Cross-partition transactions** need two-phase commit or a saga; design so that most
  transactions touch one partition.
- **Unique constraints and auto-increment ids** across shards need a global scheme (UUIDs,
  Snowflake-style ids: timestamp + machine + sequence).
- **Resharding** a live database is a project: dual writes or change data capture, backfill,
  verification, cut-over. Choose a scheme that rebalances without it.

## Summary

- Replicate for availability and reads; partition for writes and size; usually both.
- Synchronous replication protects acknowledged writes at the cost of latency; async can lose
  the newest writes on failover.
- Replica lag breaks read-your-writes, monotonic reads and consistent prefix unless you design
  for them.
- Failover needs detection, election, fencing and client reconfiguration; split brain is the
  failure to fear.
- Choose the partition key for even load and locality; use many fixed partitions so
  rebalancing moves whole partitions.

## Further reading

- Martin Kleppmann, *Designing Data-Intensive Applications*, chapters 5 and 6: <https://dataintensive.net/>
- PostgreSQL documentation, "High Availability, Load Balancing, and Replication": <https://www.postgresql.org/docs/current/high-availability.html>
- Martin Fowler's site, Unmesh Joshi, "Fixed Partitions" and "Leader and Followers": <https://martinfowler.com/articles/patterns-of-distributed-systems/fixed-partitions.html> and <https://martinfowler.com/articles/patterns-of-distributed-systems/leader-follower.html>
- Azure Architecture Center, "Sharding pattern": <https://learn.microsoft.com/en-us/azure/architecture/patterns/sharding>
- Discord engineering, "How Discord Stores Trillions of Messages": <https://discord.com/blog/how-discord-stores-trillions-of-messages>
