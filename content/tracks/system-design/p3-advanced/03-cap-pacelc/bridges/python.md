---
from: python
status: draft
concept: "What a read is allowed to return"
from_way: "One process where a dict update is visible immediately (the GIL helps), and one database."
native_way: "Replicas that may disagree for a while; you choose the consistency model per call and pay for strength with latency or availability."
sensei_tip: "boto3's get_item(ConsistentRead=True) costs twice the read capacity of an eventually consistent read and is unavailable for global secondary indexes: a concrete price tag for consistency."
---

| Python | Distributed consistency |
| --- | --- |
| `table.get_item(..., ConsistentRead=True)` | a strong read in DynamoDB |
| cassandra-driver `ConsistencyLevel.QUORUM` | quorum reads and writes |
| `redis.set(k, v, nx=True)` | a conditional write on one leader |
| Celery results backend lag | eventual consistency in practice |
| `pymongo` `ReadConcern("majority")` | reading majority-committed data |
