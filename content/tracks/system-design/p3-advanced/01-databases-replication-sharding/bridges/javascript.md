---
from: javascript
status: draft
concept: "Copies and parts of the data"
from_way: "One connection string in an ORM, with MongoDB or PostgreSQL behind it."
native_way: "Leaders, replicas with lag, and shards keyed by a partition key; the driver or a router decides where each query goes."
sensei_tip: "MongoDB's readPreference: 'secondary' reads from replicas and may return stale data; with causal consistency sessions the driver gives you read-your-writes on secondaries."
---

| JavaScript | Replication and sharding concept |
| --- | --- |
| `readPreference: 'secondaryPreferred'` | replica reads, possibly stale |
| `session` with causal consistency | read-your-writes across replicas |
| `sh.shardCollection('db.c', { tenantId: 'hashed' })` | hash partitioning |
| Prisma read replicas extension | leader/replica routing |
| `ulid()` | sortable ids without a sequence |
