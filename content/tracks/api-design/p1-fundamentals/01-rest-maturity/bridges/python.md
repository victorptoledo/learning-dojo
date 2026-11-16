---
from: python
status: draft
concept: "Resources and REST"
from_way: "Flask routes such as `/api/create_user` called with POST."
native_way: "FastAPI `APIRouter(prefix=\"/orders\")` with standard methods per path, or Django REST framework `ViewSet`s that map list, create, retrieve, update and destroy."
sensei_tip: "DRF's `@action(detail=True, methods=[\"post\"])` generates `/orders/{pk}/cancel/`: name it as a noun (`cancellation`) to stay resource-oriented."
---

| Python | REST concept |
| --- | --- |
| `APIRouter(prefix="/orders")` | collection |
| `ModelViewSet` | standard methods on a resource |
| `@action(detail=True)` | custom action / sub-resource |
| `PATCH` with a Pydantic model of optional fields | partial update |
| `Location` header on 201 | level-3 link |
