# Consensus, Leader Election, Leases and Fencing

Many designs in this track quietly assume that "the leader", "the owner of the partition map" or
"the holder of the lock" is a single, agreed fact. Getting several machines to agree on one fact
while some of them crash and the network misbehaves is the **consensus** problem. You will rarely
implement consensus, but you will constantly depend on it, and you must know what it does and
does not protect you from.

## Why it is hard

- **No reliable failure detection**: a node that does not answer may be dead, slow, paused by GC
  or cut off. A timeout is a guess.
- **No shared clock**: clocks drift and jump (NTP corrections, VM migrations); "the lease expired
  at 10:00:05" means different instants on different machines.
- **Processes pause**: a stop-the-world GC, a swapped-out VM or a stalled disk can freeze a
  process for seconds, after which it continues as if no time had passed.

The FLP result says no deterministic algorithm can guarantee consensus in a fully asynchronous
system with even one crash; practical algorithms are always safe and make progress when the
network behaves for long enough.

## Raft in one page

Raft (used by etcd, Consul, CockroachDB, TiKV, Kafka's KRaft) keeps a replicated log on an odd
number of nodes, usually 3 or 5:

| Concept | Meaning |
| --- | --- |
| Term | a numbered epoch; at most one leader per term |
| Election | a follower that hears no heartbeat for a randomized timeout becomes a candidate and asks for votes; a majority makes it leader |
| Log replication | the leader appends entries and sends them to followers; an entry is **committed** once a majority has it |
| Safety | a candidate only wins if its log is at least as up to date as the majority's, so committed entries are never lost |
| Majority (quorum) | with 5 nodes, 3; the cluster survives 2 failures. A minority side of a partition cannot commit |

Consequences for design: writes cost a round trip to a majority (put the nodes in different
zones of one region, not across oceans, unless you accept the latency); a cluster of 2 tolerates
no failures, 3 tolerates one; adding nodes adds safety, not write throughput.

## What you use consensus for

Not for your bulk data (too slow), but for small, critical facts:

- **Leader election**: which replica of a partition is the leader.
- **Membership and configuration**: which nodes exist, the partition map, feature flags.
- **Locks and leases**: who may run the nightly job, who owns a shard.
- **Unique sequences**: monotonically increasing ids (and fencing tokens).

The usual tools: etcd, ZooKeeper, Consul, or a database row updated with a conditional write
(`UPDATE ... WHERE version = ?`) or a DynamoDB conditional put, which delegate consensus to the
database.

## Leases

A **lease** is a lock with an expiry: "node A owns shard 7 until 10:00:30". If A dies, the lease
runs out and someone else can take over without anyone having to detect the death. A must
**renew** before expiry, and must **stop acting** before it might have expired.

The trap is the pause:

```text
A acquires the lease (expires 10:00:30)
A starts a long GC pause at 10:00:20 ............................. A resumes at 10:00:45,
                                     lease expires 10:00:30        still believes it holds the lease,
                                     B acquires the lease          writes to storage → corrupts B's work
                                     B writes to storage
```

Checking "do I still hold it?" before writing does not help: the pause can happen between the
check and the write. Clock skew makes it worse.

## Fencing tokens

The fix is to let the **resource** reject stale holders. The lock service hands out a strictly
increasing **fencing token** with every lease (Raft terms and ZooKeeper's `zxid` serve this
purpose). Every write carries the token, and storage remembers the highest token it has seen:

```text
A gets token 33 ... pause ...
B gets token 34, writes with 34 → storage records 34
A wakes, writes with 33        → storage rejects: 33 < 34
```

Now safety no longer depends on clocks or pauses; the lease only affects **liveness** (how fast
someone else takes over). You implement leases with fencing in the next kata.

Practical advice:

- Use the token everywhere the old holder could do damage: storage writes, messages it sends,
  calls to other services (pass it as a header).
- Keep a **safety margin**: stop acting some time before your local view of the expiry, to absorb
  clock drift.
- Lease length is a trade-off: short leases fail over quickly but need frequent renewals and
  cause flapping on slow networks; long leases make failover slow.
- A "distributed lock" built on a cache without fencing (Redis `SET NX PX` alone, or Redlock) is
  fine for **efficiency** (avoid doing the same work twice) but not for **correctness**.

## Clocks and ordering

| Clock | Gives | Use |
| --- | --- | --- |
| Wall clock (time of day) | an approximate instant, may jump | display, rough timestamps |
| Monotonic clock | elapsed time on one machine, never jumps back | timeouts, durations, local lease checks |
| Lamport clock | a counter that respects causality | total order of events |
| Version vectors | detect concurrent updates | conflict detection in replicated data |
| Bounded-uncertainty clocks (Spanner TrueTime) | an interval guaranteed to contain now | external consistency, at the price of waiting out the uncertainty |

Never use wall-clock timestamps from different machines to decide which write came last when it
matters ("last write wins" with skewed clocks silently loses the newer write).

## Summary

- Consensus (Raft, Paxos) lets a majority agree on a log despite crashes; minorities cannot
  commit; 3 nodes tolerate 1 failure, 5 tolerate 2.
- Use it through etcd, ZooKeeper, Consul or conditional writes for small, critical facts.
- Leases bound how long a dead holder blocks others; pauses and skew make lease checks unsafe.
- Fencing tokens let the resource reject stale holders, so safety does not depend on time.
- Use monotonic clocks for durations and logical clocks or versions for ordering.

## Further reading

- The Raft site, with the paper and a visualization: <https://raft.github.io/>
- Martin Kleppmann, "How to do distributed locking" (fencing tokens): <https://martin.kleppmann.com/2016/02/08/how-to-do-distributed-locking.html>
- Amazon Builders' Library, "Leader election in distributed systems": <https://aws.amazon.com/builders-library/leader-election-in-distributed-systems/>
- Google SRE book, "Managing Critical State: Distributed Consensus for Reliability": <https://sre.google/sre-book/managing-critical-state/>
- Martin Fowler's site, Unmesh Joshi, "Lease": <https://martinfowler.com/articles/patterns-of-distributed-systems/lease.html>
- Leslie Lamport, "Time, Clocks, and the Ordering of Events in a Distributed System": <https://lamport.azurewebsites.net/pubs/time-clocks.pdf>
