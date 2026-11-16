---
from: python
status: draft
concept: "Undecidability and Python tooling"
from_way: "Expecting a type checker to prove a dynamic program correct."
native_way: "mypy and pyright check a decidable approximation (the annotated types); dynamic features such as eval and getattr escape any static analysis; Python itself is Turing complete."
sensei_tip: "Use static checks for what they can decide, tests for behaviour, and timeouts for anything that might not terminate (signal.alarm, asyncio.wait_for)."
---

| In Python | Underneath |
| --- | --- |
| mypy / pyright | sound for the typed subset, with escapes (Any) |
| `eval`, `exec` | anything at all can happen |
| `asyncio.wait_for(c, 5)` | a timeout instead of a proof |
| Hypothesis | explores inputs, does not prove |
| `sys.setrecursionlimit` | a bound, not a decision |
