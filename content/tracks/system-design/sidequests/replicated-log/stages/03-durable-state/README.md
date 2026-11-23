Nodes crash and come back. Some state must survive on disk, or a restarted node could vote
twice in one term (two leaders) or forget entries it promised to keep (lost commits).

- The **current term**, the **vote** of that term and the **log** are durable: save them with
  `storage.save(...)` whenever they change, before sending any message that depends on the
  change (a `Vote` you granted, an `AppendReply` for entries you stored, the `RequestVote`s of
  your own election). The simulator restarts a node by building a new `RaftNode` on the same
  storage: the constructor loads what `storage.load()` returns.
- Everything else is volatile and starts from scratch after a restart: the role (follower),
  the leader, the commit index (0, until a leader says more) and the timers.
- `Storage.Persistent` has room for the snapshot too; keep `Snapshot.EMPTY` there until stage 5.

The tests restart followers, leaders and the whole cluster, and check that votes and terms
are never forgotten, that a node whose log fell behind cannot win an election (the vote rule
of stage 1 is what keeps committed entries alive), and that nothing committed is lost.
