---
from: java
status: reviewed
concept: "Zero-downtime changes with Flyway and flags"
from_way: "A Flyway script that renames a column, shipped with the code in one release, during a maintenance window."
native_way: "Expand-and-contract migrations over several releases, batched backfills, CREATE INDEX CONCURRENTLY in a non-transactional migration, and flags through OpenFeature's Java SDK."
sensei_tip: "Flyway runs each script in a transaction; mark the concurrent-index migration as non-transactional, or PostgreSQL refuses it."
---

| Java | Production ownership |
| --- | --- |
| `V12__rename_column.sql` | expand, migrate, contract |
| `@Scheduled` batch backfill | small transactions |
| OpenFeature Java SDK | feature flags |
| Actuator health + canary metrics | safe rollouts |
