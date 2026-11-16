---
from: go
status: reviewed
concept: "A log that everything else is built on"
from_way: "Raft logs in etcd or Consul, where a leader appends entries and followers replicate them."
native_way: "The WAL streamed to standbys; failover is decided outside PostgreSQL, often by Patroni using etcd's Raft."
sensei_tip: "pgx accepts several hosts in one connection string with target_session_attrs=read-write, so after a failover the driver finds the new primary on reconnect."
---

| Go / Raft | PostgreSQL |
| --- | --- |
| the Raft log | the WAL |
| a log index | an LSN |
| a follower | a standby |
| a quorum write | synchronous replication with ANY 1 (...) |
| leader election | Patroni, operators or the managed service |
