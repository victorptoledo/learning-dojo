Time in Raft is cut into **terms**; each term starts with an election and has at most one
leader.

```java
public RaftNode(String id, List<String> members, RaftConfig config, Storage storage)
public void tick()
public void receive(Envelope envelope)
public List<Envelope> drainOutbox()   // the messages sent since the last call, in order
public Role role()
public long term()
public String votedFor()              // null when it voted for nobody in this term
public String leader()                // the leader it knows for this term, or null
```

`members` lists every node, this one included. Until the later stages, `log()` is empty and
`lastIndex()`, `commitIndex()` and `snapshotIndex()` are 0 (the simulator reads them).
Ignore `storage` until stage 3.

**Terms.** Every message carries its sender's term. A node that sees a higher term than its
own takes it, becomes a follower and forgets its vote (`votedFor` null) and its leader, before
handling the message. A message from a lower term is answered with the node's own term and
changes nothing else.

**Elections.**

- A follower or candidate that goes `config.electionTimeout()` ticks without hearing from the
  leader of its term (an `AppendEntries`) and without granting a vote starts an election: its
  term goes up by one, it becomes a `CANDIDATE`, votes for itself and sends a `RequestVote`
  to every other member (in the order of their names) with the index and term of the last
  entry of its log (0 and 0 for now).
- A candidate that does not win within another timeout starts a new election, in a new term.
- A node grants its vote when the request's term is its term, it has not voted for another
  node in that term, and the candidate's log is at least as up to date as its own: a higher
  last term, or the same last term and a last index at least as high. Granting a vote resets
  its election timer. It answers every `RequestVote` with a `Vote`.
- A candidate that gets votes from a majority of the members (its own included) becomes the
  `LEADER`. A cluster of one elects itself at its first timeout.

**Heartbeats.** A new leader sends an `AppendEntries` to every other member at once, and again
every `config.heartbeatInterval()` ticks. For now they are empty:
`AppendEntries(term, leader, 0, 0, [], 0)`. A follower or candidate that gets one for its term
becomes (or stays) a follower, remembers the leader, resets its election timer and answers
`AppendReply(term, itself, true, 0)`.
