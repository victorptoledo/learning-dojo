---
from: java
status: draft
concept: "Multithreading and synchronisation"
from_way: "Manual threads, `synchronized`, `ReentrantLock` and silent race conditions."
native_way: "The `Send` and `Sync` traits stop unsafe data from crossing threads; `mpsc` channels allow message passing."
sensei_tip: "A motto Rust borrows from Go: don't communicate by sharing memory; share memory by communicating."
---
