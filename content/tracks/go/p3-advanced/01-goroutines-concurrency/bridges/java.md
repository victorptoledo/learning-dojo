---
from: java
status: draft
concept: "Concurrency model"
from_way: "Operating system threads with large stacks; thousands of them can exhaust memory."
native_way: "Goroutines are managed by the Go runtime and start with a tiny stack, so hundreds of thousands can run at once."
sensei_tip: "`go myFunction()` runs it concurrently; the Go scheduler multiplexes goroutines onto the CPU cores."
---
