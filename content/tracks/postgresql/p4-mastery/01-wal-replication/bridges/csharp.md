---
from: csharp
status: reviewed
concept: "A log that everything else is built on"
from_way: "SQL Server's transaction log, Always On availability groups with synchronous and asynchronous replicas, and transactional replication."
native_way: "The WAL with streaming replication (like availability groups), logical replication (like transactional replication) and an external cluster manager for failover."
sensei_tip: "Coming from Always On: PostgreSQL has no built-in automatic failover or listener. Patroni, an operator or the managed service provides them; libpq and Npgsql can list several hosts with Target Session Attributes=read-write."
---

| SQL Server | PostgreSQL |
| --- | --- |
| transaction log, LSN | WAL, LSN |
| CHECKPOINT | checkpoint (checkpoint_timeout, max_wal_size) |
| availability group, synchronous commit | streaming replication with synchronous_standby_names |
| readable secondary | hot standby |
| transactional replication | logical replication |
| the AG listener | a VIP, DNS, proxy or multi-host connection string |
