---
from: python
status: draft
concept: "The layers under a Python program"
from_way: "Treating Python as if code ran directly, with no machine underneath."
native_way: "CPython compiles your module to bytecode (.pyc) and a C interpreter loop executes it; objects are C structs on a heap managed by reference counting, inside an OS process."
sensei_tip: "`dis.dis(f)` shows the bytecode of a function: the first layer below your code, one command away."
---

| In Python | Underneath |
| --- | --- |
| `python app.py` | a process running the CPython interpreter |
| `__pycache__/*.pyc` | cached bytecode |
| `int` | an arbitrary-precision object, not a machine word |
| `threading.Thread` | an OS thread (limited by the GIL in classic CPython) |
| `open()` | a file descriptor from the kernel |
