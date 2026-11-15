---
from: javascript
status: draft
concept: "Many concurrent tasks without callbacks"
from_way: "One event loop with promises and async/await."
native_way: "Many virtual threads that block freely, scheduled by the JVM."
sensei_tip: "Node avoids blocking at all costs; with virtual threads blocking is cheap again. The rule that remains: do not hold a lock or a pinned resource while you wait."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| await fetch(a) | a blocking HTTP call on a virtual thread |
| Promise.all | submit tasks and join them |
| p-limit | Semaphore |
| worker_threads | platform threads or a ForkJoinPool for CPU work |
