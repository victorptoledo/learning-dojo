---
from: python
status: draft
concept: "Architecture choices"
from_way: "A Django monolith, or many small FastAPI services split early."
native_way: "Boundaries by business capability and data ownership, extraction when there is a reason, and sagas across services."
sensei_tip: "Splitting a Django project into services by model rarely works; split by capability, with each service owning its tables."
---

| Python | In practice |
| --- | --- |
| Django apps sharing models | modules owning their data |
| service calls inside a request | synchronous dependency chains |
| Celery chains across services | a saga with compensations |
| shared database between services | an anti-pattern |
