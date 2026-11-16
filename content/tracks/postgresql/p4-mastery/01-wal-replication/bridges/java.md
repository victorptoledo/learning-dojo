---
from: java
status: reviewed
concept: "A log that everything else is built on"
from_way: "Kafka topics as the source of truth, with consumers that track their offsets, and a load balancer in front of application replicas."
native_way: "The WAL is the database's log: replicas stream it, replication slots are consumer offsets, and failover promotes a standby."
sensei_tip: "A replication slot is a consumer group offset that never expires: if the consumer disappears, the primary keeps the whole log for it, like a Kafka topic with infinite retention filling the disk."
---

| Java / Kafka | PostgreSQL |
| --- | --- |
| a topic partition | the WAL |
| an offset | an LSN |
| a consumer group's committed offset | a replication slot's restart_lsn |
| acks=all | synchronous replication |
| Debezium reading the binlog | logical decoding from a slot |
| a leader election in ZooKeeper | Patroni with etcd |
