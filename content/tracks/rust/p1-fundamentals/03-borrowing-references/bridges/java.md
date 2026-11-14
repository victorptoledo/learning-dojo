---
from: java
status: draft
concept: "Pass by reference vs aliasing"
from_way: "References are passed around freely, so several threads or methods can mutate the same object at once."
native_way: "Readers-writer rule: any number of shared references (`&T`) or exactly one mutable reference (`&mut T`), never both at the same time."
sensei_tip: "This compile-time rule rules out data races and whole classes of pointer bugs in safe Rust."
---
