---
from: python
status: draft
concept: "Strategy and factory"
from_way: "An abstract `Strategy` class with one subclass per variant and a `StrategyFactory` with an if/elif chain."
native_way: "Functions are strategies: pass them (`sorted(items, key=...)`) or keep them in a dict that is the factory and the registry at once."
sensei_tip: "A decorator that registers functions in a dict (`@rule('express')`) gives you an open/closed registry in five lines; it is how Flask routes and pytest plugins work."
---

| Python | Pattern |
| --- | --- |
| `sorted(items, key=len)` | strategy |
| `RULES = {"express": express, ...}` | registry |
| `@classmethod def from_json(cls, ...)` | factory method |
| entry points in `pyproject.toml` | plugin registry |
| `enum.Enum` members with methods | closed set of strategies |
