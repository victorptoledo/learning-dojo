---
from: python
status: draft
concept: "Hexagonal and clean architecture"
from_way: "Business rules in Django views or model methods, with the ORM and `requests` called from everywhere."
native_way: "A domain package of dataclasses and functions with no framework imports, `Protocol` classes as ports, adapters for Django, SQLAlchemy and HTTP clients, a bootstrap module as composition root, and `import-linter` contracts."
sensei_tip: "*Architecture Patterns with Python* (Percival and Gregory) builds exactly this in Python, with a fake repository and unit of work for the tests."
---

| Python | Hexagonal idea |
| --- | --- |
| `@dataclass` domain objects | the core |
| `typing.Protocol` | driven port |
| FastAPI routes, Celery tasks | driving adapters |
| SQLAlchemy repository class | driven adapter |
| a `bootstrap.py` that wires everything | composition root |
| `import-linter` `forbidden` contract | the dependency rule as a check |
