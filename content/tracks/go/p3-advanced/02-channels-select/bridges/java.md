---
from: java
status: draft
concept: "Concurrent communication"
from_way: "Blocking queues or shared variables guarded by locks and wait/notify."
native_way: "Channels (`ch := make(chan int)`) send and receive values; `select` waits on several channels at once."
sensei_tip: "Combine `select` with a timeout channel (`time.After`) to avoid hanging forever on external calls."
---
