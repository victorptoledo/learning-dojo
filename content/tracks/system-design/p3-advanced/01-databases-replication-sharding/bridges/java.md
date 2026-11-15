---
from: java
status: draft
concept: "Copies and parts of the data"
from_way: "One DataSource pointing at one database, with @Transactional covering everything."
native_way: "Writes go to a leader, reads may go to lagging replicas, and data is split by a partition key, so routing, lag and cross-shard work become design concerns."
sensei_tip: "Spring's AbstractRoutingDataSource routes to a leader or replica per call; @Transactional(readOnly = true) is a common signal for the replica. Remember read-your-writes: a user's read right after a write should still go to the leader."
---

| Java / Spring | Replication and sharding concept |
| --- | --- |
| `AbstractRoutingDataSource` | routing to leader, replica or shard |
| `@Transactional(readOnly = true)` | a read that may use a replica |
| `UUID.randomUUID()` or a Snowflake-style id | ids that do not need one sequence |
| Hibernate multi-tenancy (`TenantIdentifierResolver`) | tenant-based routing |
| Debezium (CDC) | streaming changes to move or copy data |
