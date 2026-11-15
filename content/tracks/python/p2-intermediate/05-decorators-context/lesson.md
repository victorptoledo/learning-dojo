# Decorators and Context Managers

Two features make Python code short without making it magic. **Decorators** add behaviour
around a function (logging, timing, retries, caching, routing) without touching its body.
**Context managers** guarantee that setup is followed by cleanup, whatever happens in between.

## Decorators are functions

A decorator is a callable that takes a function and returns a replacement. The `@` line is
only syntax:

```python
@timed
def load(path): ...

# is exactly
def load(path): ...
load = timed(load)
```

The replacement is usually a **wrapper** closure that calls the original:

```python
import functools
import time

def timed(func):
    @functools.wraps(func)                  # copy __name__, __doc__, __module__, add __wrapped__
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        try:
            return func(*args, **kwargs)
        finally:
            elapsed = time.perf_counter() - start
            print(f"{func.__name__} took {elapsed:.3f} s")
    return wrapper
```

Without `functools.wraps`, every decorated function would be called `wrapper` in logs,
tracebacks, `help()` and test reports. Always use it.

### Decorators with arguments

`@retry(times=3)` first **calls** `retry(times=3)`, which must return the real decorator. So
a decorator with arguments has three levels:

```python
def retry(times: int = 3, on: type[Exception] = Exception):
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except on:
                    if attempt == times:
                        raise
        return wrapper
    return decorator

@retry(times=5, on=ConnectionError)
def fetch(url): ...
```

### Stacking

Decorators apply **bottom up**: the one closest to `def` wraps first, so the top one runs
first at call time.

```python
@app.get("/orders")   # 2. registers the cached function as a route
@cache                # 1. wraps list_orders first
def list_orders(): ...
```

### Typing a decorator

`ParamSpec` (written `**P` in the new syntax) keeps the decorated function's signature, so
callers still get completion and type errors:

```python
from collections.abc import Callable

def timed[**P, R](func: Callable[P, R]) -> Callable[P, R]:
    @functools.wraps(func)
    def wrapper(*args: P.args, **kwargs: P.kwargs) -> R:
        ...
        return func(*args, **kwargs)
    return wrapper
```

### Decorators you already use

`@property`, `@classmethod`, `@staticmethod`, `@dataclass`, `@functools.cache`,
`@pytest.fixture`, `@app.get(...)` in web frameworks: all ordinary decorators. Class
decorators (like `@dataclass`) receive a class and return a class.

### Caching

```python
from functools import cache, lru_cache

@cache                      # unbounded memoisation
def fib(n: int) -> int:
    return n if n < 2 else fib(n - 1) + fib(n - 2)

@lru_cache(maxsize=1024)    # bounded: evicts the least recently used
def exchange_rate(currency: str) -> Decimal: ...

exchange_rate.cache_info()   # hits, misses, size
exchange_rate.cache_clear()
```

Arguments must be hashable, and a cache on a method keeps `self` (and so the object) alive.
Cache only pure functions, or results you are allowed to serve stale.

## Context managers

The `with` statement calls `__enter__` on the way in and `__exit__` on the way out, **even
when the block raises or returns**:

```python
with open(path, encoding="utf-8") as file:   # file = the value __enter__ returned
    data = file.read()
# the file is closed here, in every case
```

A class-based context manager:

```python
class Timer:
    def __enter__(self) -> "Timer":
        self.start = time.perf_counter()
        return self

    def __exit__(self, exc_type, exc, traceback) -> bool:
        self.elapsed = time.perf_counter() - self.start
        return False        # do not suppress an exception from the block

with Timer() as t:
    work()
print(t.elapsed)
```

`__exit__` receives the exception (or three `None`s). Returning a **true** value suppresses
it, which is almost never what you want: return `False` or `None`.

### The generator shortcut

`contextlib.contextmanager` turns a generator with exactly one `yield` into a context
manager. Code before the `yield` is `__enter__`; the code after it is `__exit__`:

```python
from contextlib import contextmanager

@contextmanager
def transaction(conn):
    try:
        yield conn               # the with block runs here
    except BaseException:
        conn.rollback()
        raise                    # re-raise: the caller must see the failure
    else:
        conn.commit()
```

Put the `yield` in `try`/`finally` (or `except`/`else`) or the cleanup will not run when the
block raises.

### The contextlib toolbox

| Tool | Use it for |
| --- | --- |
| `suppress(FileNotFoundError)` | Ignoring one specific exception, on purpose and visibly |
| `ExitStack` | A variable number of resources: `stack.enter_context(open(p))` in a loop, all closed at the end |
| `closing(thing)` | Objects with `close()` but no context manager support |
| `nullcontext()` | "No context manager" when one is optional: `with lock if lock else nullcontext():` |
| `redirect_stdout(buffer)` | Capturing prints from code you do not control |
| `chdir(path)` (3.11) | Temporarily changing the working directory |

```python
from contextlib import ExitStack

with ExitStack() as stack:
    files = [stack.enter_context(open(path)) for path in paths]
    merge(files)            # every file is closed, even if opening the third one failed
```

A context manager built with `@contextmanager` (or `ContextDecorator`) also works as a
decorator: `@transaction_scope()` wraps a whole function in the `with`. Asynchronous code has
the same pair: `async with` and `@asynccontextmanager`.

## Summary

- A decorator takes a function and returns a wrapper; `@d` means `f = d(f)`.
- Always `functools.wraps`; decorators with arguments have three levels; stacks apply bottom
  up.
- Type decorators with `[**P, R]` and `Callable[P, R]`; cache pure functions with `cache` or
  `lru_cache`.
- `with` guarantees cleanup via `__enter__` and `__exit__`; do not suppress exceptions by
  accident.
- `@contextmanager` with `yield` inside `try`; `ExitStack` for many resources; `suppress` for
  deliberate ignores.

## Further reading

- Glossary, "decorator": <https://docs.python.org/3/glossary.html#term-decorator>
- `functools` (`wraps`, `cache`, `lru_cache`): <https://docs.python.org/3/library/functools.html>
- `contextlib`: <https://docs.python.org/3/library/contextlib.html>
- The `with` statement (Language Reference): <https://docs.python.org/3/reference/compound_stmts.html#the-with-statement>
- PEP 318, decorators for functions and methods: <https://peps.python.org/pep-0318/>
- PEP 343, the with statement: <https://peps.python.org/pep-0343/>
- roadmap.sh, Python topic "Decorators": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/decorators@pIluLJkySqn_gI_GykV6G.md>
