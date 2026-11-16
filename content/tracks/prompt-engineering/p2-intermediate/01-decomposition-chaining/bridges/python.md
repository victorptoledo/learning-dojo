---
from: python
status: draft
concept: "Small functions composed"
from_way: "One long function that does everything, with print statements for debugging."
native_way: "A list of steps, each a template and a parser, run in order with a check after each, a bounded retry, a token budget and a trace of what happened."
sensei_tip: "Model each step as a small dataclass (name, template, parse) and the chain as a loop over them. Then the trace is just a list you append to, and each step is unit-testable with a fake model."
---

| Python | Chaining concept |
| --- | --- |
| function composition `g(f(x))` | a sequential chain |
| `assert` or a validator after each call | a gate |
| a dict of handlers keyed by kind | routing |
| `asyncio.gather(...)` | parallel sections |
| `logging` with a record per step | the trace |
