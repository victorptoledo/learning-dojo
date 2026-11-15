---
from: javascript
status: draft
concept: "Higher-order functions, decorators and cleanup"
from_way: "Higher-order functions such as withLogging(fn), TypeScript or TC39 decorators on classes and methods, try/finally for cleanup, and using declarations with Symbol.dispose."
native_way: "Decorators with @ on any function or class, functools.wraps to keep the name, and with statements calling __enter__ and __exit__."
sensei_tip: "A Python decorator is exactly a higher-order function like withLogging(fn), with @ syntax that works on plain functions too. The new JavaScript using declaration is Python's with."
---

| JavaScript / TypeScript | Python |
| --- | --- |
| `const logged = withLogging(fn)` | `@with_logging` above `def fn` |
| `@memoize` method decorator | `@functools.cache` |
| keeping `fn.name` by hand | `@functools.wraps(func)` |
| `try { ... } finally { file.close() }` | `with open(path) as file:` |
| `using res = acquire()` with `[Symbol.dispose]` | `with acquire() as res:` with `__exit__` |
| `await using` | `async with` |
