---
from: java
status: draft
concept: "The operating system underneath"
from_way: "Treating the runtime as a layer that hides the operating system."
native_way: "Understand real Linux processes: file descriptors, POSIX signals (`SIGTERM`, `SIGKILL`), namespaces and I/O buffers."
sensei_tip: "When a container receives `SIGTERM`, the app has a few seconds to close connections and drain pools before the inevitable `SIGKILL`."
---
