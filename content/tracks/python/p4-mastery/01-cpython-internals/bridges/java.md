---
from: java
status: draft
concept: "Two virtual machines, two memory models"
from_way: "javac to class files, a JVM that interprets then JIT-compiles hot methods, a tracing garbage collector (G1, ZGC) with no reference counts, Integer caching from -128 to 127, and javap to read bytecode."
native_way: "The compiler to code objects at import, a specialising interpreter with an experimental JIT, reference counting plus a cycle collector, small-int caching from -5 to 256, and dis to read bytecode."
sensei_tip: "Integer == Integer surprises Java developers above 127; int is int surprises Python developers above 256. Same lesson: identity is not equality."
---

| Java / JVM | Python / CPython |
| --- | --- |
| `.class` files | `.pyc` files in `__pycache__` |
| `javap -c` | `dis.dis(func)` |
| interpreter + C1/C2 JIT | specialising interpreter + experimental JIT |
| tracing GC (G1, ZGC) | reference counting + generational cycle collector |
| `try-with-resources` needed for timely close | refcounting closes on the last reference (still use `with`) |
| `Integer` cache -128..127 | small-int cache -5..256 |
| `String.intern()` | `sys.intern()` |
| JNI | the C API, the stable ABI (`abi3`) |
