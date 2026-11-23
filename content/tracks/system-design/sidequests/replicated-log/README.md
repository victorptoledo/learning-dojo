# Sidequest: Replicated log with Raft

etcd, Consul, CockroachDB and Kafka's KRaft keep their most important data in a log
replicated by **Raft**: a majority of nodes agrees on every entry, a leader is elected when
the old one disappears, and nothing committed is ever lost, whatever the network does. Here you
write a simplified Raft node and watch a simulated cluster try to break it.

Build `dojo.raft.RaftNode`. It is a **pure state machine**: it changes only when the cluster
calls `tick()` (one unit of time) or `receive(envelope)` (a message arrived), and it never
touches a network or a clock. The messages it wants to send wait in its outbox until the
cluster takes them with `drainOutbox()`. That is what makes it testable: the tests run a
simulator that ticks every node, delivers messages in a fixed order, cuts the network into
partitions, drops messages, crashes nodes and restarts them from their disk, and after
every tick checks the two promises of Raft:

- **Election safety:** at most one leader per term.
- **State machine safety:** once an entry is committed at an index, every node that commits
  that index has the same entry there, forever.

The starter gives you the contract types: the messages (`Msg` and its records), `Envelope`,
`Entry`, `Role`, `RaftConfig`, `Storage`, `Snapshot` and `NotLeaderException`, and the method
signatures of `RaftNode`. Log indexes start at 1; index 0 is "before the first entry", with
term 0.

Simplified means: no cluster membership changes, no pre-vote, timeouts fixed per node (the
simulator gives each node a different one, so elections are deterministic), and the leader
sends every entry a follower is missing in one message.

Use the JDK only.
