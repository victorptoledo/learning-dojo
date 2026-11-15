---
from: go
status: draft
concept: "Compiled runtime versus interpreter"
from_way: "Ahead-of-time compilation to machine code, a concurrent mark-and-sweep GC tuned with GOGC, goroutines scheduled on all cores by the runtime, and go tool objdump to read the output."
native_way: "Compilation to bytecode at import and a specialising interpreter, reference counting plus a cycle collector, a GIL (or the free-threaded build) deciding parallelism, and dis to read bytecode."
sensei_tip: "Go's runtime was designed for parallel goroutines from day one; CPython is retrofitting that with the free-threaded build, which is why extension support matters so much there."
---

| Go | CPython |
| --- | --- |
| machine code from `go build` | bytecode from the compiler, cached in `.pyc` |
| `go tool objdump` | `dis` |
| concurrent mark-and-sweep GC, `GOGC` | refcounting + cycle collector, `gc.set_threshold` |
| `runtime.GC()` | `gc.collect()` |
| goroutines on every core | GIL per interpreter; free-threaded build for parallel threads |
| cgo | the C API, stable ABI wheels |
