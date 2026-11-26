---
from: python
status: draft
concept: "A cache with an eviction policy"
from_way: "functools.lru_cache, a bounded collections.deque(maxlen=...), and logging that rotates old files."
native_way: "Treat the context like a bounded cache: keep recent turns verbatim, evict old tool results to placeholders, compact old history into a summary, and keep durable facts in notes outside it."
sensei_tip: "A frozen dataclass per turn plus dataclasses.replace(turn, content=placeholder) clears a result without mutating the history you were given, which makes compaction easy to test."
---

| Python | Context-engineering concept |
| --- | --- |
| `deque(maxlen=n)` | keeping only the recent turns verbatim |
| `lru_cache` eviction | clearing old tool results |
| `dataclasses.replace` | a placeholder that keeps the call id |
| a `NOTES.md` the script appends to | structured notes outside the window |
| `concurrent.futures` workers with their own state | sub-agents with fresh contexts |
