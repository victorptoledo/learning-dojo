---
from: java
status: draft
concept: "A runtime for one language, with an event loop instead of a thread per request"
from_way: "The JVM runs bytecode compiled by javac; servlet containers give each request a thread from a pool, and blocking I/O blocks that thread."
native_way: "Node.js runs JavaScript (and stripped TypeScript) on V8; one thread runs your code and libuv waits for I/O, so a request that waits holds no thread."
sensei_tip: "Think of Node.js as Netty or Vert.x as the default model: everything is non-blocking, and a blocking call (a long loop, a sync file read in a handler) stalls the whole event loop, not one thread."
---

| Java | Node.js |
| --- | --- |
| JVM (HotSpot JIT, GC) | V8 (JIT, GC) |
| NIO / Netty event loop | libuv event loop, the default for everything |
| Thread pool per request (Tomcat) | One main thread + libuv thread pool |
| Virtual threads (Java 21+) | async/await on the event loop |
| LTS releases (17, 21, 25) | LTS releases (even majors: 22, 24, 26) |
| `java Main.java` (single-file launch) | `node main.ts` (type stripping) |
