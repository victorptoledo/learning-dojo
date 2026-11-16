---
from: java
status: draft
concept: "Virtual memory and the JVM"
from_way: "Reading -Xmx as the memory the process will use."
native_way: "The JVM reserves the heap as virtual address space and the OS backs it with RAM on first touch; metaspace, code cache, thread stacks and direct buffers come on top, so RSS exceeds the heap."
sensei_tip: "Size the heap with -XX:MaxRAMPercentage in containers, and use Native Memory Tracking (-XX:NativeMemoryTracking=summary) to see the non-heap part."
---

| In Java | Underneath |
| --- | --- |
| `-Xmx2g` | reserves address space; RAM comes on touch |
| `-XX:+AlwaysPreTouch` | touches every heap page at start |
| `-XX:+UseLargePages` | huge pages, fewer TLB misses |
| `FileChannel.map` | mmap of a file |
| `OutOfMemoryError` vs OOMKilled | the heap limit vs the kernel's limit |
