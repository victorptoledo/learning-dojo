---
from: python
status: draft
concept: "Adapter, decorator, proxy and facade"
from_way: "Monkey-patching a library's methods, or subclassing it to change one method."
native_way: "Adapter classes around SDKs, function decorators (`@retry`, `@lru_cache`) and wrapper objects with `__getattr__` forwarding for proxies; a small module as a facade."
sensei_tip: "Python's `@decorator` syntax is the decorator pattern for functions: `@functools.lru_cache` wraps a function in caching without touching it. For objects, wrap and forward; do not patch."
---

| Python | Pattern |
| --- | --- |
| a class translating an SDK's dicts to your dataclasses | adapter |
| `@functools.lru_cache`, `@retry` | decorator (functions) |
| `__getattr__` forwarding wrapper | proxy |
| `unittest.mock.Mock(wraps=obj)` | spying proxy |
| `requests.get` over `urllib3` | facade |
