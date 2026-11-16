---
from: python
status: draft
concept: "The API as a contract"
from_way: "Returning `model.__dict__` or an ORM object serialised by a generic encoder."
native_way: "Pydantic models (FastAPI) or serializers (Django REST framework) as the response schema, separate from ORM models."
sensei_tip: "FastAPI's `response_model=` filters the output to the declared fields; use it even when the ORM object has more."
---

| Python | API concept |
| --- | --- |
| SQLAlchemy / Django model | implementation |
| Pydantic `BaseModel` as `response_model` | representation in the contract |
| FastAPI's generated `/openapi.json` | contract to review, not to ignore |
| `dict(row)` in a view | an accidental contract |
