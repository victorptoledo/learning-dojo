A **consumer group** shares the partitions of a topic among its members: each partition is
read by exactly one member, and the group remembers, per partition, the **committed offset**
where it should go on after a crash.

```java
public void join(String group, String topic, String member)
public void leave(String group, String member)
public int generation(String group)
public Map<Integer, String> assignment(String group)   // partition -> member, sorted
public Batch poll(String group, String member, int max)
public void commit(String group, String member, int generation, int partition, long offset)
public long committed(String group, int partition)      // -1 when never committed
```

`Batch(int generation, List<Message> messages)` and `CommitFailedException` are new in the
starter.

**Membership and assignment**

- A group reads one topic: joining it with another topic throws `IllegalArgumentException`.
  Joining as a member that is already in changes nothing.
- Every change of members is a **rebalance**: the generation goes up by one (the first join
  makes it 1). Partitions are assigned by ranges: members sorted by name, each gets
  `partitions / members` partitions in a row and the first `partitions % members` get one
  more. With 5 partitions and members `a`, `b`: `a` gets 0, 1, 2 and `b` gets 3, 4.
- A member that did not `poll` for `config.sessionTimeout()` is dead: the next call about the
  group (any of the methods above) removes it, which is a rebalance. `leave` removes a member
  at once.

**Polling and committing**

- After a rebalance, a member reads each of its partitions from the group's committed offset
  (or `startOffset` when there is none, or when the commit is older than what retention kept).
  So messages read but not committed are read again by the next owner: **at-least-once**.
- `poll` returns up to `max` messages from the member's partitions, the lowest partition
  first, and moves its positions past them. It throws `IllegalStateException` for a member
  that is not in the group (a dead member must join again).
- `commit` records `offset` (the next one to read) for the partition. It throws
  `CommitFailedException`, and records nothing, when the generation is not the current one or
  the partition is not the member's: a **zombie** that lost its partitions in a rebalance
  must not move the offsets of the member that has them now. An offset outside the partition
  throws `IllegalArgumentException`.
- Groups are independent: each has its own members, positions and commits.
