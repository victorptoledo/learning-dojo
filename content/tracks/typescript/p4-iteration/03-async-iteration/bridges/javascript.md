---
from: javascript
status: draft
concept: "The same async iteration, typed"
from_way: "async generators and for await over streams, with nothing saying what a stream yields: Buffer, string or objects."
native_way: "The same runtime, with AsyncIterable<T> and AsyncGenerator<T, TReturn, TNext> types; Node.js streams are typed AsyncIterable<any>, so validate or annotate what you read."
sensei_tip: "A Readable stream yields any. Wrap it in a small async generator that declares what it yields (string lines, parsed records), and the rest of the pipeline is typed."
---

| JavaScript | TypeScript |
| --- | --- |
| `async function* lines(stream)` | `async function* lines(stream: AsyncIterable<Uint8Array \| string>): AsyncGenerator<string, void, undefined>` |
| `for await (const chunk of stream)` | The same; `chunk` is `any` from a `Readable` |
| A helper that takes arrays or streams | `source: AsyncIterable<T> \| Iterable<T>` |
| `signal` passed around by convention | `signal?: AbortSignal` in the options type |
