---
from: python
status: draft
concept: "Repository and unit of work"
from_way: "Django models called from views (`Order.objects.filter(...)`, `order.save()`) everywhere."
native_way: "Django's ORM is active record; for richer domains, a repository protocol with a SQLAlchemy implementation, and SQLAlchemy's `Session` as the unit of work with `session.begin()` as its boundary."
sensei_tip: "SQLAlchemy's Session is a unit of work and identity map, just like JPA's persistence context: `session.get(Account, 42)` twice returns the same object."
---

| Python | Pattern |
| --- | --- |
| `class Accounts(Protocol)` | repository interface |
| Django `Model.save()` | active record |
| SQLAlchemy `Session` | unit of work, identity map |
| `with session.begin():` | transaction boundary |
| a dict-backed fake | in-memory repository for tests |
