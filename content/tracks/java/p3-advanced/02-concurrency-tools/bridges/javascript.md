---
from: javascript
status: draft
concept: "Coordinating threads safely"
from_way: "A single thread with an event loop, so shared-memory races are rare."
native_way: "Real threads sharing memory, with tools to coordinate them."
sensei_tip: "Java's CompletableFuture is a promise: thenApply is then, thenCompose chains, allOf is Promise.all. Unlike JavaScript, the callbacks may run on another thread."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| Promise.then | thenApply / thenCompose |
| Promise.all | CompletableFuture.allOf |
| Atomics.add | AtomicInteger.incrementAndGet |
| Map shared by workers | ConcurrentHashMap |
