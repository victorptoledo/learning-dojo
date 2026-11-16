---
from: javascript
status: reviewed
concept: "Readers that never block writers"
from_way: "Immutable updates ({ ...state, balance: 80 }) in React or Redux, where old objects live until nothing references them."
native_way: "MVCC: every update writes a new row version, each transaction reads the versions its snapshot allows, and VACUUM collects the garbage."
sensei_tip: "An await on a slow API inside prisma.$transaction keeps a snapshot open. Do the slow work first, then open a short transaction."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| { ...row, balance: 80 } | UPDATE writes a new tuple |
| a closure holding the old object | a snapshot seeing old versions |
| V8's garbage collector | VACUUM |
| await fetch(...) inside a transaction | idle in transaction |
| structural sharing | HOT: the new version stays on the same page and indexes are reused |
