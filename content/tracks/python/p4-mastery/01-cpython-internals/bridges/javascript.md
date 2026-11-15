---
from: javascript
status: draft
concept: "V8 versus CPython"
from_way: "V8 parses to bytecode for the Ignition interpreter, then optimises hot code with Sparkplug, Maglev and TurboFan, a generational tracing GC, hidden classes for property access, and one thread per isolate."
native_way: "CPython compiles to bytecode and specialises hot instructions, with an experimental JIT, reference counting plus a cycle collector, dict-based (or slotted) attributes, and one GIL per interpreter."
sensei_tip: "A V8 isolate is close to a CPython subinterpreter: separate heaps, no shared objects, messages passed between them."
---

| V8 / Node.js | CPython |
| --- | --- |
| Ignition bytecode | code objects, readable with `dis` |
| TurboFan optimising JIT | specialising interpreter, experimental JIT |
| generational tracing GC | reference counting + cycle collector |
| hidden classes / inline caches | specialised attribute instructions, `__slots__` |
| isolates and worker threads | subinterpreters, processes, free-threaded threads |
| N-API | the C API and stable ABI |
