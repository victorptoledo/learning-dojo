A log that grows forever fills the disk and makes a restart replay everything. Compact it.

```java
public long snapshotIndex()   // the last index the snapshot covers (0 without one)
```

**Taking snapshots.** When `config.snapshotEvery()` is above 0 and a node has applied that
many entries since its last snapshot, it takes one at `lastApplied`:
`Snapshot(index, term of that entry, data, highest sequence per client)`. The entries up to
that index leave the log (`log()` returns only the ones after it) and the snapshot is saved
in `Storage.Persistent` with the rest. Indexes stay absolute: `lastIndex()` and the index
`propose` returns keep counting from the start.

**Restarting.** A node that restarts loads the snapshot: its data, client sequences, commit
index and last applied index start at the snapshot.

**Followers that fall behind.** When the next entry a follower needs is already in the
leader's snapshot, the leader sends `InstallSnapshot(term, leader, snapshot)` instead of an
`AppendEntries`. The follower treats it like an `AppendEntries` for its term (leader, timer),
then, if the snapshot is newer than its own:

- it keeps the log entries after the snapshot when its log has the snapshot's last entry
  (same index and term), and drops its whole log otherwise;
- it replaces its data and client sequences with the snapshot's (unless it already applied
  further), moves its commit index up to the snapshot and saves everything;

and answers `AppendReply(term, itself, true, snapshot index)`, after which the leader goes on
with `AppendEntries` from there.
