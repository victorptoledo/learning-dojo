---
from: java
status: draft
concept: "The layers under a Java program"
from_way: "Thinking of the JVM as the bottom of the world: write Java, the JVM runs it."
native_way: "javac emits bytecode, the JVM interprets it and HotSpot JIT-compiles hot methods to machine code, which runs as threads of an OS process, using virtual memory, caches and the network like any native program."
sensei_tip: "When a Java service is slow, ask which layer is busy: GC (runtime), threads waiting (OS), round trips (network) or misses (memory). JFR records all of them at once."
---

| In Java | Underneath |
| --- | --- |
| `javac Foo.java` | compiler: source to bytecode |
| `java Foo` | a process; the JVM interprets, then JIT-compiles |
| `new Thread()` | an OS thread scheduled by the kernel |
| `Socket` | a file descriptor over TCP/IP |
| `double` | IEEE 754 binary64 in a register |
