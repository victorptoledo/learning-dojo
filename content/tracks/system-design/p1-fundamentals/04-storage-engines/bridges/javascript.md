---
from: javascript
status: draft
concept: "How data is stored and found"
from_way: "Mongoose documents or Prisma models, with arrays and objects stored as JSON."
native_way: "Model the data around the reads you serve, know the engine behind the store, and keep blobs and analytics in stores made for them."
sensei_tip: "MongoDB's WiredTiger uses B-trees and supports compound indexes with the same leftmost-prefix rule as SQL: an index on { customerId: 1, createdAt: -1 } does not help a query on createdAt alone."
---

| JavaScript | Storage concept |
| --- | --- |
| `schema.index({ customerId: 1, createdAt: -1 })` | a compound index |
| embedded subdocuments | the document model (read together) |
| `IndexedDB` | a key-value store in the browser |
| `@aws-sdk/client-s3` | object storage for files |
| `level` (LevelDB) | an LSM key-value engine |
