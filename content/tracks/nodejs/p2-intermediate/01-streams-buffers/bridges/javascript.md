---
from: javascript
status: draft
concept: "Node streams next to Web Streams"
from_way: "fetch's response.body is a ReadableStream; you read it with a reader or for await, and pipeThrough a TransformStream."
native_way: "Node streams (Readable, Writable, Transform) are the standard in Node APIs, connected with pipeline; Web Streams also exist and convert with Readable.fromWeb and toWeb."
sensei_tip: "The concepts map one to one: pipeThrough is a Transform in pipeline, and the desiredSize of a Web Stream is highWaterMark. Use Node streams with Node APIs and convert at the edges."
---

| Web Streams | Node streams |
| --- | --- |
| `ReadableStream` | `Readable` |
| `WritableStream` | `Writable` |
| `TransformStream` | `Transform` |
| `rs.pipeThrough(t).pipeTo(ws)` | `pipeline(r, t, w)` |
| `TextDecoderStream` | `setEncoding("utf8")` / `StringDecoder` |
| `Uint8Array` | `Buffer` (a `Uint8Array` subclass) |
