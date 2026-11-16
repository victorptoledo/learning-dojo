---
from: python
status: draft
concept: "Layered architecture"
from_way: "Django's `views.py`, `models.py` and `serializers.py` per app, with business rules spread over views, model methods and signals."
native_way: "Feature apps or packages with a thin view, a services module for use cases and the ORM behind them, and `import-linter` layers contracts where layers are kept."
sensei_tip: "A Django app is already a feature package. Keep business rules in a services module inside it, and let views only translate HTTP."
---

| Python | Layering idea |
| --- | --- |
| Django views, FastAPI routers | presentation layer |
| a `services.py` per app | application layer |
| models and querysets | persistence layer |
| `import-linter` `layers` contract | closed layers as a check |
| one Django app per feature | package by feature |
| a view that only calls `Model.objects.create` | sinkhole (often fine for CRUD) |
