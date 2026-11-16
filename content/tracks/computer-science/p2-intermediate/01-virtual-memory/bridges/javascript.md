---
from: javascript
status: draft
concept: "Virtual memory and Node.js"
from_way: "Assuming the V8 heap is all the memory a Node process uses."
native_way: "V8 reserves virtual space for its heap generations; Buffers live outside the V8 heap; process.memoryUsage reports rss, heapUsed and external separately."
sensei_tip: "Set --max-old-space-size below the container limit, and watch `rss` and `external`, not only `heapUsed`."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| `process.memoryUsage().rss` | resident memory |
| `--max-old-space-size` | the old-generation heap limit |
| `Buffer.allocUnsafe` | memory outside the V8 heap |
| `heap out of memory` | V8's own limit, before the kernel's |
| WebAssembly memory | a linear address space inside the process |
