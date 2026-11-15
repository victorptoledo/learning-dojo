---
from: python
status: draft
concept: "Decorators and context managers, the modern way"
from_way: "Wrappers without functools.wraps, memoisation dicts by hand, try/finally blocks around every resource, nested with statements for many files, and decorators typed as Callable[..., Any]."
native_way: "functools.wraps always, functools.cache and lru_cache, @contextmanager for small context managers, ExitStack and parenthesised multi-item with, and ParamSpec to keep signatures."
sensei_tip: "Since 3.10 a with statement can list several managers in parentheses over several lines, which removes most deeply nested with blocks; ExitStack covers the rest."
---

| Older habit | Python 3.15 |
| --- | --- |
| `def wrapper(*a, **k)` without `@wraps(func)` | `@functools.wraps(func)` |
| `_cache = {}` and `if n in _cache` | `@functools.cache` |
| `f = open(p)` / `try:` ... `finally: f.close()` | `with open(p) as f:` |
| nested `with a: with b: with c:` | `with (open(a) as fa, open(b) as fb):` or `ExitStack` |
| a class with `__enter__`/`__exit__` for a few lines | `@contextmanager` with `yield` in `try` |
| `except FileNotFoundError: pass` | `with suppress(FileNotFoundError):` |
| `Callable[..., Any]` for a decorator | `def deco[**P, R](f: Callable[P, R]) -> Callable[P, R]` |
