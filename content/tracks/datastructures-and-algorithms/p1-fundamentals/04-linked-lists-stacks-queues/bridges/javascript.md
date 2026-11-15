---
from: javascript
status: draft
concept: "Lists, stacks and queues"
from_way: "Using an array as a queue with push and shift, which is O(n) per shift."
native_way: "An array is a fine stack with push and pop. JavaScript has no built-in queue or deque, so for large queues keep a head index into an array (and compact it now and then) or write a small ring buffer."
sensei_tip: "A queue with `let head = 0; q.push(x); const x = q[head++];` is O(1) per operation; slice off the consumed part when `head` gets large. This is the standard BFS queue in performance-sensitive JavaScript."
---

| JavaScript | Java | Note |
| --- | --- | --- |
| `arr.push(x)` / `arr.pop()` | `push` / `pop` | stack |
| `arr.shift()` | `list.remove(0)` | O(n): avoid as a queue |
| array + head index | `ArrayDeque.poll` | O(1) queue |
| hand-written ring buffer | `ArrayDeque` / `ArrayBlockingQueue` | fixed capacity |
| `{ val, next }` objects | `Node` class | linked list nodes |
