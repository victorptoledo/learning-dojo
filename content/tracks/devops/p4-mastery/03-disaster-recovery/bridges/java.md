---
from: java
status: draft
concept: "Recovery is designed, tested and timed"
from_way: "A DBA takes nightly dumps of the database the Java application uses, and nobody has tried to restore one."
native_way: "Point-in-time recovery meets the RPO, a scheduled job restores into a test environment and runs checks, and the measured time is the RTO."
sensei_tip: "Test what happens to HikariCP and your transactions during a database failover: retries and connection validation decide whether users notice."
---

| Java world | Disaster recovery |
| --- | --- |
| nightly `pg_dump` | point-in-time recovery |
| a standby app server | warm standby |
| Resilience4j retries | surviving a failover |
| a manual restore guide | an automated restore test |
