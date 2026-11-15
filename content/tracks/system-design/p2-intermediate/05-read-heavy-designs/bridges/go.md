---
from: go
status: draft
concept: "Making reads cheap"
from_way: "Handlers that run several queries and assemble the response struct per request."
native_way: "Precomputed inboxes, denormalised documents and projections, merged and filtered at read time."
sensei_tip: "Merging a precomputed inbox with a few celebrity lists is a k-way merge; container/heap does it in O(n log k) without sorting everything."
---

| Go | Read-heavy concept |
| --- | --- |
| several `db.Query` calls per handler | joins at read time |
| `container/heap` | k-way merge of timelines |
| `rdb.ZAdd` / `ZRevRange` | inbox in a Redis sorted set |
| a consumer goroutine of change events | a projector |
| `WHERE (created_at, id) < ($1, $2)` | keyset pagination |
