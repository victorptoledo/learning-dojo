---
from: java
status: draft
concept: "Non-blocking I/O on a single thread"
from_way: "One thread per request, each blocked until the database answers."
native_way: "A single thread runs JavaScript while asynchronous I/O (disk, network) is delegated to libuv and its thread pool."
sensei_tip: "Never block the main thread: heavy CPU work (hashing, parsing a huge JSON) freezes every user of the server."
---
