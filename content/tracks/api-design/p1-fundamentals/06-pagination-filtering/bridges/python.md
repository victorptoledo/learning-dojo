---
from: python
status: draft
concept: "Pagination, filtering and sorting"
from_way: "`Model.objects.all()[offset:offset+limit]` or returning the whole queryset."
native_way: "DRF's `CursorPagination` or a hand-written keyset query in SQLAlchemy; filters validated by Pydantic query models."
sensei_tip: "DRF's `CursorPagination` needs an `ordering` on a unique, unchanging field; ordering by `-created` alone can skip rows with equal timestamps."
---

| Python | Collection concept |
| --- | --- |
| `LimitOffsetPagination` | offset |
| `CursorPagination` | keyset cursor |
| `.order_by("-created_at", "-id")` | tie-breaker |
| `Query(le=100)` | maximum limit |
| `base64.urlsafe_b64encode` | opaque cursor |
