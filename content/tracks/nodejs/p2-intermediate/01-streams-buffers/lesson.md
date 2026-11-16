# Streams and Buffers: Processing Files Without Exhausting Memory

`readFile` on a 3 GB file needs 3 GB of memory, and the first byte of an answer waits for the
last byte of the input. **Streams** process data piece by piece, as it arrives, with a few
kilobytes of memory. Files, sockets, HTTP requests and responses, child process pipes,
compression and hashing are all streams in Node.js.

## Buffers

A `Buffer` is a fixed-size chunk of raw bytes (a `Uint8Array` with extra methods):

```ts
const greeting = Buffer.from("olá", "utf8");   // <Buffer 6f 6c c3 a1>: 4 bytes, 3 characters
greeting.length;                                // 4 (bytes, not characters)
greeting.toString("utf8");                      // "olá"
greeting.toString("base64");                    // "b2zDoQ=="
Buffer.from("b2zDoQ==", "base64");              // back to the bytes
Buffer.concat([a, b]);                          // join chunks
Buffer.byteLength("olá");                        // 4: use this for Content-Length
```

Text is bytes plus an **encoding**. In UTF-8 a character takes 1 to 4 bytes, so a chunk
boundary can fall **in the middle of a character**. Calling `chunk.toString()` on each chunk
then produces garbage (`�`). A `StringDecoder` (from `node:string_decoder`), or
`stream.setEncoding("utf8")`, keeps the incomplete bytes for the next chunk.

## Four kinds of streams

| Type | Does | Examples |
| --- | --- | --- |
| `Readable` | Produces data | `fs.createReadStream`, an HTTP request, `process.stdin` |
| `Writable` | Consumes data | `fs.createWriteStream`, an HTTP response, `process.stdout` |
| `Duplex` | Both, independent sides | A TCP socket |
| `Transform` | A Duplex whose output is computed from its input | `zlib.createGzip()`, `crypto.createHash()`, a CSV parser |

Byte streams carry `Buffer`s (or strings). **Object mode** streams carry any JavaScript value,
one object per chunk: rows from a database, parsed log entries.

## Reading

The simplest way to read a stream is `for await`:

```ts
import { createReadStream } from "node:fs";

let lines = 0;
for await (const chunk of createReadStream("huge.log")) {   // Buffer chunks of 64 KiB
  for (const byte of chunk) if (byte === 10) lines += 1;
}
```

`node:readline` gives lines, and `node:stream/consumers` collects a whole (small) stream:
`await text(stream)`, `await json(stream)`, `await buffer(stream)`. `Readable.from(iterable)`
turns an array or a generator into a stream.

## Writing your own Transform

```ts
import { Transform } from "node:stream";

const upperCase = new Transform({
  transform(chunk: Buffer, _encoding, callback) {
    callback(null, chunk.toString("utf8").toUpperCase());  // push one value and continue
  },
});

const toJsonLines = new Transform({
  writableObjectMode: true,                // objects in, strings out
  transform(row: Record<string, unknown>, _encoding, callback) {
    callback(null, `${JSON.stringify(row)}\n`);
  },
  flush(callback) {                        // the input ended: push anything left
    callback();
  },
});
```

`callback(error)` fails the stream; `callback()` pushes nothing; `this.push(x)` pushes extra
values. For simple cases, Node.js also has async-iterator helpers: `readable.map(fn)`,
`.filter(fn)`, `.take(n)` (still experimental in places), and you can pass an async generator
function as a stage of `pipeline`.

## pipeline, not pipe

```ts
import { createReadStream, createWriteStream } from "node:fs";
import { pipeline } from "node:stream/promises";
import { createGzip } from "node:zlib";

await pipeline(
  createReadStream("access.log"),
  createGzip(),
  createWriteStream("access.log.gz"),
);
```

`pipeline` connects the streams, **propagates errors** (any failure destroys every stream and
rejects the promise) and resolves when the last one finishes. The older `a.pipe(b)` does none
of the error handling: if `a` fails, `b` stays open, file descriptors leak, and nothing tells
you. Use `pipeline` from `node:stream/promises`.

## Backpressure

What if the reader is faster than the writer, for example a local disk feeding a slow mobile
client? Every stream has an internal buffer limited by **`highWaterMark`** (64 KiB for byte
streams since Node.js 22, 16 objects in object mode):

1. `writable.write(chunk)` returns **`false`** when its buffer is full.
2. The producer should **stop** and wait for the `"drain"` event.
3. `pipeline` and `pipe` do this for you: the readable pauses, and the source (the file, the
   socket) is not read any further until the slow side catches up.

So memory stays bounded **whatever the sizes**. You break this when you ignore the return
value of `write()`, or collect chunks into an array "to process later".

```ts
for (const row of rows) {
  if (!out.write(format(row))) {
    await once(out, "drain");      // writing by hand: respect backpressure yourself
  }
}
```

## Web Streams

Node.js also implements the browser's **Web Streams** (`ReadableStream`, `WritableStream`,
`TransformStream`), used by `fetch`: `response.body` is a `ReadableStream`. Convert with
`Readable.fromWeb(webStream)` and `Readable.toWeb(nodeStream)`. In Node.js code, Node streams
are still the common choice; Web Streams help when the same code runs in the browser, Deno or
an edge runtime.

## Summary

- `Buffer` = bytes; text needs an encoding, and characters can cross chunk boundaries.
- Readable, Writable, Duplex, Transform; byte or object mode.
- Read with `for await`; write Transforms with `transform` and `flush`.
- Connect with `pipeline` from `node:stream/promises`: errors propagate, everything is
  cleaned up.
- Backpressure (`write()` returns `false`, `"drain"`) keeps memory bounded.

## Further reading

- Node.js API, "Stream": <https://nodejs.org/api/stream.html>
- Node.js, "Backpressuring in Streams": <https://nodejs.org/en/learn/modules/backpressuring-in-streams>
- Node.js API, "Buffer": <https://nodejs.org/api/buffer.html>
- Node.js API, "String decoder": <https://nodejs.org/api/string_decoder.html>
- Node.js API, `stream.pipeline()`: <https://nodejs.org/api/stream.html#streampipelinesource-transforms-destination-options>
- Node.js API, "Web Streams API": <https://nodejs.org/api/webstreams.html>
- James Halliday, "stream-handbook" (classic, partly dated): <https://github.com/substack/stream-handbook>
