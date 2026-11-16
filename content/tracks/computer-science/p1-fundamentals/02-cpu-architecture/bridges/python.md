---
from: python
status: draft
concept: "What the CPU does with Python code"
from_way: "Expecting Python code to be fast because the CPU is fast."
native_way: "Each bytecode instruction runs dozens to hundreds of machine instructions in CPython's evaluation loop, with indirect branches the predictor struggles with; NumPy moves loops into compiled, vectorised C."
sensei_tip: "For number crunching, move the loop into NumPy (SIMD in C) or run it under PyPy's JIT; `sys.setrecursionlimit` guards the C stack."
---

| In Python | Underneath |
| --- | --- |
| `for x in data:` | the interpreter dispatch loop, per element |
| `numpy.sum(a)` | one tight vectorised machine-code loop |
| `RecursionError` | protects the call stack |
| `dis.dis(f)` | the virtual instructions |
| Python 3.13+ JIT (experimental) | copies machine-code templates for hot bytecode |
