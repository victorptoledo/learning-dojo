---
from: csharp
status: draft
concept: "Streams and pipelines"
from_way: "System.IO.Stream with CopyToAsync, GZipStream wrapping a stream, and System.IO.Pipelines for high-performance parsing with backpressure."
native_way: "Readable, Writable and Transform streams with pipeline; zlib transforms; backpressure built in through highWaterMark and drain."
sensei_tip: "await source.CopyToAsync(destination) is pipeline(source, destination), and decorators like GZipStream become Transform stages in the middle."
---

| C# | Node.js |
| --- | --- |
| `Stream` (read side) | `Readable` |
| `Stream` (write side) | `Writable` |
| `new GZipStream(s, Compress)` | `zlib.createGzip()` |
| `CopyToAsync` | `pipeline` |
| `PipeReader` / `PipeWriter` | Streams with `highWaterMark` |
| `byte[]` / `Memory<byte>` | `Buffer` |
