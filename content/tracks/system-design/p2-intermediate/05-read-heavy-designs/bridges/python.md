---
from: python
status: draft
concept: "Making reads cheap"
from_way: "Django querysets with select_related and prefetch_related, computed on every request."
native_way: "Precompute per-user lists and per-screen documents at write time, filter at read time, and paginate with cursors."
sensei_tip: "Django REST Framework's CursorPagination implements keyset pagination with an opaque cursor; it needs a unique, ordered field such as (created, id)."
---

| Python | Read-heavy concept |
| --- | --- |
| `select_related` / `prefetch_related` | joins at read time |
| `post_save` signal updating a summary | a (fragile) denormalised copy |
| Redis `ZADD inbox:{user} ts post_id` | a bounded inbox (sorted set) |
| DRF `CursorPagination` | keyset pagination |
| Celery task per post | asynchronous fan-out |
