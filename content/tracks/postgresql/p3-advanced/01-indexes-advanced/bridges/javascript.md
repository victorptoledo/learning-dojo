---
from: javascript
status: reviewed
concept: "Choosing an index structure"
from_way: "Map and Set lookups, sorted arrays, and @@index([field]) in a Prisma schema."
native_way: "Six index types for six shapes of data and query, plus partial and expression indexes."
sensei_tip: "Prisma supports type: Gin, Gist, Brin and others in @@index for PostgreSQL, but not partial or expression indexes: add those in a custom SQL migration."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| a sorted array with binary search | B-tree |
| Map | Hash index |
| Map<word, Set<id>> | GIN |
| a quadtree library | GiST / SP-GiST |
| min and max per page of results | BRIN |
| @@index([tags], type: Gin) | CREATE INDEX ... USING gin (tags) |
