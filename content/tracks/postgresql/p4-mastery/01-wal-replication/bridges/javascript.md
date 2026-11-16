---
from: javascript
status: reviewed
concept: "A log that everything else is built on"
from_way: "Event streams and change streams (MongoDB), read replicas behind a provider's endpoint."
native_way: "The WAL is the database's log: standbys replay it, logical replication decodes it into row changes, and failover promotes a standby."
sensei_tip: "Prisma and node-postgres can use separate read and write URLs; reads from a replica can lag. Read your own writes from the primary."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| MongoDB change streams | logical replication or logical decoding |
| a resume token | an LSN in a replication slot |
| a read-replica URL | a hot standby |
| a write concern of majority | synchronous replication |
| fs.fsyncSync | the WAL flush at COMMIT |
