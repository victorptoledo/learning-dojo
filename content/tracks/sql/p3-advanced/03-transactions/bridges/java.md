---
from: java
status: draft
concept: "Concurrent changes that stay correct"
from_way: "synchronized blocks, ReentrantLock and AtomicInteger inside one JVM, and @Transactional around service methods."
native_way: "Transactions, isolation levels and row locks, which work across every process and server."
sensei_tip: "A synchronized block protects one JVM; with two instances behind a load balancer only the database can serialise the change. Use an atomic UPDATE or SELECT ... FOR UPDATE."
---

| Java | SQL |
| --- | --- |
| @Transactional | BEGIN ... COMMIT (rollback on exception) |
| atomicInteger.addAndGet(-80) with a check | UPDATE ... SET balance = balance - 80 WHERE balance >= 80 |
| lock.lock() | SELECT ... FOR UPDATE |
| @Version on an entity | UPDATE ... WHERE version = :read_version |
| @Transactional(isolation = SERIALIZABLE) | BEGIN ISOLATION LEVEL SERIALIZABLE, plus a retry on 40001 |
