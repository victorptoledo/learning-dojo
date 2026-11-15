---
from: java
status: draft
concept: "Cross-cutting behaviour and deterministic cleanup"
from_way: "Annotations processed by frameworks through proxies or AOP (@Transactional, @Cacheable, @Retryable), and try-with-resources over AutoCloseable."
native_way: "Decorators are plain functions that wrap functions at definition time, and with statements call __enter__ and __exit__ on any context manager."
sensei_tip: "A Python decorator is an aspect you can read: no proxy, no framework, just a closure. A Java annotation is only metadata until a framework acts on it; @transactional in Python is the code itself."
---

| Java | Python |
| --- | --- |
| `@Cacheable` | `@functools.cache` / `@lru_cache(maxsize=...)` |
| `@Retryable(maxAttempts = 3)` | a `@retry(times=3)` decorator |
| `@Transactional` | a `transaction()` context manager or decorator |
| `try (var in = Files.newInputStream(p)) { }` | `with open(p, "rb") as f:` |
| `AutoCloseable.close()` | `__exit__` |
| `addSuppressed` on close failures | `ExitStack` keeps closing the rest |
| a dynamic proxy | a wrapper closure with `functools.wraps` |
