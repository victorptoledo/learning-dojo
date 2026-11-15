---
from: java
status: draft
concept: "What a read is allowed to return"
from_way: "The Java Memory Model: volatile and synchronized give happens-before, so threads see each other's writes in a defined order."
native_way: "The same question across machines: linearizable, causal or eventual consistency, paid for with coordination and latency, and chosen per operation."
sensei_tip: "A volatile field is linearizable within one JVM; a value in a replicated store is not unless the store says so. Read the driver's consistency settings (Cassandra's ConsistencyLevel.QUORUM, DynamoDB's ConsistentRead) the way you read the JMM."
---

| Java | Distributed consistency |
| --- | --- |
| `volatile` / `synchronized` (happens-before) | linearizable reads and writes |
| `AtomicLong.compareAndSet` | a conditional write (compare-and-set) |
| Cassandra driver `ConsistencyLevel.QUORUM` | W and R of a quorum |
| DynamoDB `ConsistentRead(true)` | a strongly consistent read |
| `ConcurrentHashMap` weakly consistent iterators | eventual views during updates |
