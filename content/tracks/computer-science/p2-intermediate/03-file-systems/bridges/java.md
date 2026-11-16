---
from: java
status: draft
concept: "Files and durability in Java"
from_way: "Trusting that close() or flush() puts data on disk."
native_way: "flush() moves bytes from Java buffers to the OS (the page cache); FileChannel.force and FileDescriptor.sync call fsync; Files.move with ATOMIC_MOVE is rename."
sensei_tip: "For crash-safe replacement: write a temp file, `channel.force(true)`, `Files.move(tmp, target, ATOMIC_MOVE)`, then force the directory (open it as a FileChannel on Linux)."
---

| In Java | Underneath |
| --- | --- |
| `BufferedWriter.flush()` | into the page cache, not the disk |
| `FileChannel.force(true)` | fsync |
| `Files.move(..., ATOMIC_MOVE)` | rename(2) |
| `StandardOpenOption.DSYNC` | O_DSYNC writes |
| `WatchService` | inotify on Linux |
