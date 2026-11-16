---
from: java
status: draft
concept: "Processing in chunks"
from_way: "Files.readAllBytes loading a 2 GB file at once, or InputStream/OutputStream copied in a loop, and java.util.stream for lazy collections."
native_way: "Streams process data in small chunks as they arrive, using only a few kilobytes of memory, and pipeline connects them with error handling and backpressure."
sensei_tip: "Backpressure is when the reader is faster than the writer: Node pauses reading until the buffer drains, avoiding memory blow-ups. It is the same idea as Reactive Streams' request(n), built into every Node stream."
---

| Java | Node.js |
| --- | --- |
| `InputStream` | `Readable` |
| `OutputStream` | `Writable` |
| `GZIPOutputStream` wrapping a stream | `zlib.createGzip()` Transform |
| `in.transferTo(out)` | `pipeline(in, out)` |
| Reactive Streams `request(n)` | `highWaterMark`, `write()` → `false`, `"drain"` |
| `byte[]` / `ByteBuffer` | `Buffer` |
