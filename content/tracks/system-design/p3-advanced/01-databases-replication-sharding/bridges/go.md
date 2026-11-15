---
from: go
status: draft
concept: "Copies and parts of the data"
from_way: "One *sql.DB and a SERIAL primary key."
native_way: "Separate pools for leader and replicas, a shard lookup per key, and ids generated without a central sequence."
sensei_tip: "Vitess, which shards MySQL behind a SQL-speaking proxy, is written in Go; its VSchema and vindexes are a good real-world study of partition keys and routing."
---

| Go | Replication and sharding concept |
| --- | --- |
| two `*sql.DB` (leader, replica) | read/write splitting |
| `pgx` `target_session_attrs=read-write` | finding the current leader |
| a `map[int]*sql.DB` by partition | shard routing |
| `github.com/bwmarrin/snowflake` | Snowflake-style ids |
| Vitess VTGate | a routing tier |
