---
from: python
status: draft
concept: "Tactical DDD"
from_way: "Django models with every field writable from views, and business rules in view functions."
native_way: "Frozen dataclasses for value objects, plain classes for aggregate roots with methods that check invariants, a repository per root (the Django ORM or SQLAlchemy behind it), and a list of events the root records."
sensei_tip: "`@dataclass(frozen=True)` gives you immutable value objects with equality by value in one line; add a `__post_init__` to validate them."
---

| Python | Tactical DDD idea |
| --- | --- |
| `@dataclass(frozen=True) class Money` | value object |
| `NewType("OrderId", str)` or a frozen dataclass | identity as a value |
| a class whose methods guard invariants | aggregate root |
| a repository class per root | repository per aggregate |
| `self.events.append(OrderSubmitted(...))` | domain events recorded by the root |
| a version column checked on save | concurrency on the aggregate boundary |
