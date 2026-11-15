---
from: javascript
status: draft
concept: "What a read is allowed to return"
from_way: "One event loop where every assignment is seen immediately, and optimistic UI updates in the browser."
native_way: "Replicated data with explicit read and write concerns; optimistic UIs are eventual consistency you already practise."
sensei_tip: "An optimistic UI update is read-your-writes faked on the client: show the change at once, reconcile when the server answers. It is fine for a like, dangerous for a payment."
---

| JavaScript | Distributed consistency |
| --- | --- |
| MongoDB `writeConcern: { w: 'majority' }` | W = majority |
| MongoDB `readConcern: 'linearizable'` | linearizable reads, slower |
| optimistic UI updates | eventual consistency with local echo |
| `@aws-sdk` `ConsistentRead: true` | strongly consistent DynamoDB read |
| Yjs / Automerge | CRDTs that always merge |
