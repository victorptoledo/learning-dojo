# Session-Consistent Store

A key-value store is split into **partitions**. Each one has a **leader** that takes the writes
and **followers** that copy the leader's replication log asynchronously. Reading from the
followers spreads the load, but it lets a user miss their own write or see time go backwards.
A failover can also lose writes the leader had already acknowledged. You write
`ReplicatedStore`, the router in front of all of it, so that each session gets its guarantees
and nobody ever reads a lost write.

`Node`, `LogEntry`, `Token`, `Session`, `Failover`, `Hashing` and the three exceptions are
given: do not change them. A `Node` keeps a log of `LogEntry(epoch, position, key, value)` with
positions 1, 2, 3, ... It can `append` (as a leader), `apply` the next entry (as a follower),
list `entriesAfter`, `get` a value and `truncateTo` a position. A node that is down throws
`NodeDownException`, except for `position()`, `lastEpoch()` and `reads()`.

```java
var store = new ReplicatedStore(List.of(List.of(a1, a2, a3), List.of(b1, b2, b3)));
Session ada = store.session();
store.put(ada, "profile:ada", "v2");   // position 7 on the leader of its partition
store.get(ada, "profile:ada");         // Optional[v2], even if no follower has it yet
```

## Partitions and writes

- `ReplicatedStore(List<List<Node>> partitions)`: partition `i` is served by list `i`. Its first
  node leads **epoch 1**, which starts at position 0. An empty list, an empty partition or a node
  name used twice throws `IllegalArgumentException`.
- `partitionFor(key)` is `Math.floorMod(Hashing.hash64(key), partitions)`.
- `leaderOf(p)`, `epochOf(p)`: a partition that does not exist throws `IllegalArgumentException`.
- `put(session, key, value)` appends on the partition's leader with the current epoch, sets the
  session's token for that partition to `Token(epoch, position)` and returns the position. If
  the leader is down, it throws `LeaderUnavailableException`. Writes never go anywhere else.

## Replication

- `replicate(maxEntries)` (at least 1): for every partition whose leader is up, each follower
  that is up gets up to `maxEntries` entries after its own position, in order, through `apply`.
  It returns how many entries were shipped in total. The position works like a consumer's offset
  in the leader's log.
- `lag(node)`: the leader's position minus the node's (0 for the leader itself). An unknown
  name throws `IllegalArgumentException`.

## Reads with session guarantees

`get(session, key)` reads the partition from the **first follower in list order** that is up,
holds no lost writes (see below) and has a position of **at least** the session's token. If no
follower qualifies, it reads from the leader, and if the leader is down too, it throws
`LeaderUnavailableException`. Afterwards the session's token becomes `Token(current epoch,
max(token position, position of the node that answered))`.

That one rule gives you **read-your-writes** (a follower without your write is never used) and
**monotonic reads** (the token only moves forward, so a later read never goes to a node further
behind). A session without a token for the partition takes the first follower that is up and
usable, even a stale one. While the leader is down, the followers keep serving every session
they can.

## Failover

`failover(p)` throws `IllegalStateException` if the leader is up. Otherwise, it promotes the
usable follower that is up and has the **highest position** (ties: the earlier in the list), or
throws `LeaderUnavailableException` (and changes nothing) if there is none. The partition moves
to the next epoch, which **starts at the new leader's position**. The method returns
`Failover(partition, oldLeader, newLeader, newEpoch, lostWrites)`, where `lostWrites` is how far
the old leader was ahead of the new one: writes that were acknowledged and are now gone.

### What survived

The positions of epoch `e` that are still part of the history are those up to the **smallest
start of every later epoch** (all of them, if `e` is the current epoch). That rule decides two
things:

- **Nodes.** A node whose last entry is from epoch `e` and whose position is beyond what
  survived of `e` holds lost writes. Such a node never answers a read and is never promoted.
  When `replicate` reaches it, it is cut back with `truncateTo(survived)` and then caught up like
  any follower. Count the dropped entries in `truncatedEntries()`.
- **Sessions.** A session whose token `(e, position)` is from an older epoch and beyond what
  survived of `e` saw a write that no longer exists. No guarantee can be kept for it, so every
  `get` and `put` on that partition throws `LostWriteException`. The client starts a new
  session. A token that survived is still honoured.

Example: epoch 2 starts at 5 and epoch 3 at 3 (the second failover promoted a node that was
behind). Of epoch 1, only positions up to 3 survived. A session that wrote position 4 in epoch 1
is told about the loss, and a node with epoch-1 entries up to 5 is cut back to 3.

## What the grading tests check

Routing and positions per partition, replication in order and within the limit, both session
guarantees with the reads counted on each node, reads while the leader is down, the choice and
report of a failover, sessions and nodes across one failover and across a chain of them,
partitions failing over independently, and that the store keeps no static state.

> Tip: per partition, keep the list of nodes, the current leader and a `List<Long>` with the
> start of each epoch (epoch `e` is at index `e - 1`). One helper `survived(epoch)` and one
> `usable(node)` (`node.position() <= survived(node.lastEpoch())`) answer every question above.
