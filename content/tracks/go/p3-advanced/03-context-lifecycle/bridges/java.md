---
from: java
status: draft
concept: "Cascading cancellation and timeouts"
from_way: "Thread interrupts, volatile boolean flags or framework-specific cancellation tokens."
native_way: "`ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)`; when time runs out, every child operation stops."
sensei_tip: "Golden rule of production Go: the first argument of almost every function that does I/O is `ctx context.Context`."
---
