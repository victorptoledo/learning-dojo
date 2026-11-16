---
from: python
status: draft
concept: "Software architecture"
from_way: "Architecture means the framework: a Django project with apps, or a Flask app grown one blueprint at a time."
native_way: "Architecture as the decisions that are expensive to change (how the code is split, who owns the data, sync calls or events, what is deployed together), driven by the quality attributes this system needs."
sensei_tip: "Python makes everything importable from everywhere, so the module structure is only what you decide and enforce. Decide it early, because dynamic imports make it hard to recover later."
---

| Python world | Architectural question behind it |
| --- | --- |
| one Django project or several services | deployment units, availability, team independence |
| Django apps, packages, `import-linter` contracts | module structure and allowed dependencies |
| one shared ORM model used everywhere | data ownership and coupling |
| Celery tasks, message consumers | synchronous versus event-driven runtime structure |
| Gunicorn workers, async with uvicorn | performance and scalability choices |
| Locust load tests | checking performance scenarios |
