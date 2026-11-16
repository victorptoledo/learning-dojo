---
from: java
status: draft
concept: "What the CPU does with Java code"
from_way: "Assuming the JVM runs bytecode one instruction at a time."
native_way: "HotSpot compiles hot methods to machine code that the CPU pipelines, predicts and reorders; each thread has its own stack of frames, and deep recursion throws StackOverflowError."
sensei_tip: "`-XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly` (with hsdis) shows the generated machine code; JMH with `-prof perfasm` points at the hot instructions."
---

| In Java | Underneath |
| --- | --- |
| a method call | call/ret and a stack frame, often removed by inlining |
| `if` on random data | branch mispredictions |
| `Math.max(a, b)` | often a branch-free conditional move |
| loops over arrays | auto-vectorised SIMD by C2 |
| `-Xss` | the thread stack size |
