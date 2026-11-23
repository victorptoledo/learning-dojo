The leader now accepts commands and copies them to the followers.

```java
public long propose(String command)   // the index of the new entry
public List<Entry> log()
public long lastIndex()
public long commitIndex()
```

**Proposing.** `propose` on the leader appends `Entry(term, command)` and returns its index,
then sends `AppendEntries` to every follower at once. On a node that is not the leader it
throws `NotLeaderException` with the leader it knows (or null).

**Replicating.** The leader keeps, for each follower, the next index to send. An
`AppendEntries` holds every entry from that index on, the index and term of the entry just
before them (`prevLogIndex`, `prevLogTerm`) and the leader's commit index. Heartbeats are the
same message: they carry whatever the follower is missing.

- A follower that has no entry at `prevLogIndex`, or one with another term, answers
  `success = false` with a hint in `matchIndex`: the lower of its last index and
  `prevLogIndex - 1`. The leader then moves that follower's next index down (to the hint + 1,
  and at least one lower than before) and sends again at once.
- Otherwise the follower appends the entries it does not have. An entry that conflicts with
  one of its own (same index, other term) deletes that entry and everything after it first;
  entries it already has stay untouched. It answers `success = true` with the index of the
  last entry the message covered.
- The leader answers a success by recording that index for the follower, and sends again at
  once when the follower is still missing entries.

**Committing.** An entry is committed when it is stored on a majority. The leader moves its
commit index to the highest index that a majority (itself included) has **and** whose entry
is from the leader's current term: entries of older terms are committed only with them (the
Raft paper's Figure 8 shows how counting replicas of an old term loses data). Followers move
their commit index to the leader's, but never past the last entry the message covered.
