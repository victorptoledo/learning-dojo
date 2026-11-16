---
from: python
status: draft
concept: "Iterating over data instead of loading it"
from_way: "Iterating over a file object line by line, generators that yield chunks, shutil.copyfileobj, and asyncio StreamReader with drain()."
native_way: "Readable streams consumed with for await, Transform streams (or async generators) as processing stages, pipeline to connect them, and write() plus drain for backpressure."
sensei_tip: "asyncio's await writer.drain() is exactly Node's backpressure: wait when the buffer is full. pipeline does it for you, so you rarely write it by hand."
---

| Python | Node.js |
| --- | --- |
| `for line in open(p)` | `for await (const line of createInterface({ input }))` |
| Generator pipeline | `pipeline(source, transform, sink)` |
| `shutil.copyfileobj(a, b)` | `pipeline(a, b)` |
| `await writer.drain()` | `await once(out, "drain")` |
| `bytes` / `bytearray` | `Buffer` |
| `codecs.getincrementaldecoder("utf-8")` | `StringDecoder` |
