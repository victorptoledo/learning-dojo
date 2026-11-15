---
from: go
status: draft
concept: "Lists, stacks and queues"
from_way: "Using container/list for everything, or re-slicing a queue forever so its backing array never shrinks."
native_way: "A slice is a fine stack (`append` and `s = s[:len(s)-1]`). A queue can be a slice with `q = q[1:]`, but the array behind it is only freed when the slice is copied; a buffered channel is a ready-made bounded FIFO between goroutines."
sensei_tip: "`ch := make(chan T, 10_000)` is a thread-safe ring buffer with blocking send and receive: Go's ArrayBlockingQueue. `container/list` is a doubly linked list, handy for an LRU cache but slower than slices for plain queues."
---

| Go | Java | Note |
| --- | --- | --- |
| `s = append(s, x)`; `s = s[:len(s)-1]` | `push` / `pop` | stack |
| `q = append(q, x)`; `x, q = q[0], q[1:]` | `offer` / `poll` | queue (copy now and then) |
| `make(chan T, n)` | `ArrayBlockingQueue` | bounded, goroutine safe |
| `container/list` | `LinkedList` | doubly linked |
| `container/ring` | ring buffer | circular list |
