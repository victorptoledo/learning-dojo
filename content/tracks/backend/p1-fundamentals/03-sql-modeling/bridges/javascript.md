---
from: javascript
status: draft
concept: "Thinking in tables"
from_way: "Prisma or TypeORM schemas, or document-shaped JSON thinking."
native_way: "Relational design: normalised tables, keys and constraints."
sensei_tip: "A Prisma schema maps to tables, but @@index and @@unique are where performance and correctness live. Review the SQL migration Prisma generates."
---

| JavaScript / TypeScript | In practice |
| --- | --- |
| nested JSON objects | related tables |
| @relation | foreign key |
| @@unique([a, b]) | composite UNIQUE |
| @@index([status, createdAt]) | composite index, order matters |
