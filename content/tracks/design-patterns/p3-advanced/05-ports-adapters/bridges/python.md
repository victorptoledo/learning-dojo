---
from: python
status: draft
concept: "Ports and adapters"
from_way: "Django views that hold business rules and call vendor SDKs directly."
native_way: "A `domain` package with dataclasses, services and `Protocol` ports; adapters for the ORM, HTTP clients and the web framework; wiring in the app factory; import-linter to enforce the rule."
sensei_tip: "import-linter can forbid `myapp.domain` from importing `myapp.adapters`, `django` or `requests`: the ArchUnit of Python, run in CI."
---

| Python | Ports and adapters |
| --- | --- |
| `class ExchangeRates(Protocol)` | driven port |
| FastAPI or Django views | driving adapters |
| SQLAlchemy repository class | driven adapter |
| `create_app()` | composition root |
| import-linter contracts | enforcing the dependency rule |
