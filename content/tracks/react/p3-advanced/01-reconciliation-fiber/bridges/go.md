---
from: go
status: draft
concept: "A cooperative scheduler for rendering"
from_way: "The Go runtime schedules goroutines preemptively; on the server you render a whole template at once."
native_way: "React splits rendering into fibers and schedules them cooperatively on one thread: it yields to the browser between units and can drop stale work."
sensei_tip: "Fiber is a hand-built scheduler, like goroutines on a single thread that yield explicitly. Render code must be pure so a 'goroutine' can be killed and restarted at any time."
---

| Go | React |
| --- | --- |
| Goroutine | Unit of work (a fiber) |
| Scheduler preemption | React yields between fibers |
| Cancel via context | Discard a stale render |
| Render whole template | Render, then commit |
| Struct identity by key in a map | Child identity by `key` |
| Priority queues (by hand) | Urgent vs transition lanes |
