---
from: java
status: draft
concept: "Processing in chunks"
from_way: "`fs.readFileSync()` loading a 2 GB file into memory at once."
native_way: "Streams process data in small chunks as they arrive, using only a few megabytes of memory."
sensei_tip: "Backpressure is when the reader is faster than the writer: Node pauses reading until the buffer drains, avoiding memory blow-ups."
---
