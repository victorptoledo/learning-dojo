---
from: python
status: draft
concept: "Structuring a Spring application"
from_way: "Django apps or packages per feature, with imports that can reach anything."
native_way: "A modular monolith: one module per business capability, public APIs, events between modules, verified with Spring Modulith."
sensei_tip: "Django apps are modules, but nothing stops one app from importing another's models. Spring Modulith tests fail when a module reaches into another's internals."
---

| Python | Spring Boot 4 |
| --- | --- |
| Django app per feature | a top-level package per module |
| signals between apps | application events |
| import-linter | Spring Modulith verification |
| services.py as the public API | the module's public package |
