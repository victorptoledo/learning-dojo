---
from: java
status: draft
concept: "Cross-cutting concerns and deterministic cleanup"
from_way: "Framework annotations processed by bytecode generation, or try-with-resources blocks."
native_way: "Decorators are functions that take a function and return a modified one; context managers (`__enter__`/`__exit__`) guarantee cleanup of connections."
sensei_tip: "Write quick context managers with `@contextlib.contextmanager` instead of a full class."
---
