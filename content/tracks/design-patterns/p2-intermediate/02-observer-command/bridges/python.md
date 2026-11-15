---
from: python
status: draft
concept: "Observer, command and chain of responsibility"
from_way: "Hard-coded calls to every interested module, or a global list of callbacks without unsubscribe."
native_way: "Lists of callables with a returned unsubscribe function, signal libraries (blinker, Django signals), commands as callables or small dataclasses, and middleware chains."
sensei_tip: "Django signals are observer, Django middleware is chain of responsibility, and a callable class with `__call__` and `undo` is a command."
---

| Python | Pattern |
| --- | --- |
| `listeners.append(fn)` / returned `unsubscribe` | observer |
| Django `post_save` signals | observer |
| `functools.partial(fn, args)` | command (deferred call) |
| WSGI/ASGI middleware | chain of responsibility |
| `functools.reduce(lambda v, f: f(v), steps, x)` | pipeline |
