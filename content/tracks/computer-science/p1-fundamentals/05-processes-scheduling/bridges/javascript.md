---
from: javascript
status: draft
concept: "Processes and threads in JavaScript"
from_way: "Thinking JavaScript cannot do concurrency."
native_way: "Your code runs on one event-loop thread; I/O happens in the OS and libuv's thread pool; worker_threads add OS threads with separate isolates; child_process starts processes."
sensei_tip: "Never block the event loop with CPU work: move it to a Worker, and use `child_process.spawn` for other programs."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| event loop | one thread running callbacks in turn |
| `new Worker(file)` | an OS thread with its own heap |
| `child_process.spawn` | fork/exec of a child process |
| `setImmediate` / `process.nextTick` | queue ordering on the loop |
| `UV_THREADPOOL_SIZE` | threads for fs and DNS work |
