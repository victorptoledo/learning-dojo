---
from: go
status: draft
concept: "Readers, writers and composition"
from_way: "io.Reader and io.Writer interfaces, io.Copy, wrapping readers (gzip.NewWriter, bufio.Scanner), and blocking reads that give natural backpressure."
native_way: "Readable and Writable streams, pipeline instead of io.Copy, Transform streams instead of wrapping readers, and backpressure through write() returning false."
sensei_tip: "In Go, a blocked Write blocks the goroutine, so backpressure is free. In Node.js nothing blocks, so the stream machinery (highWaterMark, drain) does that job, and pipeline is your io.Copy."
---

| Go | Node.js |
| --- | --- |
| `io.Reader` | `Readable` |
| `io.Writer` | `Writable` |
| `io.Copy(dst, src)` | `pipeline(src, dst)` |
| `gzip.NewWriter(w)` | `zlib.createGzip()` in the pipeline |
| `bufio.Scanner` | `readline` |
| `[]byte` | `Buffer` |
