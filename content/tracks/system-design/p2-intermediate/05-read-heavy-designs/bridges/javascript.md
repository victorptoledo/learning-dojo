---
from: javascript
status: draft
concept: "Making reads cheap"
from_way: "GraphQL resolvers or Prisma includes assembling a page from many tables per request."
native_way: "Precompute inboxes and denormalised read models; resolvers read one document and a cursor."
sensei_tip: "The Relay cursor connection spec (edges, pageInfo, endCursor) is keyset pagination with a standard shape; DataLoader batches the remaining lookups so a feed page does not cause N+1 queries."
---

| JavaScript | Read-heavy concept |
| --- | --- |
| nested Prisma `include` | joins at read time |
| DataLoader | batching lookups per request |
| `ioredis.zrevrangebyscore` | reading an inbox newest first |
| Relay connections (`after: cursor`) | cursor pagination |
| BullMQ fan-out job | asynchronous push to inboxes |
