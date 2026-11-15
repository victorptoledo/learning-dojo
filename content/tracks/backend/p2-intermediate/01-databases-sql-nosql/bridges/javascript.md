---
from: javascript
status: draft
concept: "Persistence model"
from_way: "Prisma or Mongoose calls, often read then write in application code."
native_way: "Atomic statements, transactions with the right isolation, and locking."
sensei_tip: "Prisma's update with { decrement: 1 } and a where condition is atomic; reading the row, changing it in JavaScript and saving it is not."
---

| JavaScript / TypeScript | In practice |
| --- | --- |
| findUnique then update | a race |
| update({ data: { seats: { decrement: 1 } } }) | atomic update |
| $transaction with isolationLevel | isolation levels |
| Mongoose findOneAndUpdate | atomic document update |
