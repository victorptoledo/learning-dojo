---
from: javascript
status: draft
concept: "Concurrent changes that stay correct"
from_way: "A single-threaded event loop, where awaits between a read and a write still interleave requests."
native_way: "Transactions and row locks that serialise changes across connections and servers."
sensei_tip: "await between SELECT and UPDATE lets another request run in the gap: the lost update happens in Node too. Use a transaction with FOR UPDATE or one atomic UPDATE."
---

| JavaScript | SQL |
| --- | --- |
| await prisma.$transaction(async (tx) => {...}) | BEGIN ... COMMIT |
| read, await, write | the lost update race |
| prisma.account.update({ data: { balance: { decrement: 80 } } }) | SET balance = balance - 80 |
| a version field checked in where | optimistic concurrency |
| catch code '40001' and retry | serialization failure handling |
