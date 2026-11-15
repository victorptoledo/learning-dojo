---
from: python
status: draft
concept: "Dependency injection"
from_way: "Modules that import and instantiate their collaborators at the top, patched with `unittest.mock.patch` in every test."
native_way: "Pass collaborators to `__init__` or as function parameters, wire them in one place (the app factory), and use FastAPI's `Depends` where the framework offers DI."
sensei_tip: "If your tests are full of `mock.patch('module.Client')`, the code is using imports as a service locator. Pass the client in and the patching disappears."
---

| Python | DI idea |
| --- | --- |
| `def __init__(self, accounts: Accounts)` | constructor injection |
| `create_app()` factory | composition root |
| FastAPI `Depends(get_db)` | framework DI |
| `mock.patch(...)` everywhere | sign of hidden dependencies |
| `dependency-injector` library | container |
