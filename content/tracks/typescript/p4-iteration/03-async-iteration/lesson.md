# Async Iteration, Streams and Backpressure

Files, sockets, database cursors and paginated APIs deliver data **over time**. Async
iteration is the iteration protocol with promises: the consumer awaits each value, and the
producer only makes the next one when asked. That pull model gives you backpressure for free,
and it is how modern Node.js code reads streams. This lesson covers async iterables and async
generators, the Node.js sources that speak the protocol, early exit and cancellation, and
running work concurrently without losing order.

## The async protocol

```ts
interface AsyncIterator<T, TReturn = any, TNext = any> {
  next(...[value]: [] | [TNext]): Promise<IteratorResult<T, TReturn>>;
  return?(value?: TReturn): Promise<IteratorResult<T, TReturn>>;
}
interface AsyncIterable<T> { [Symbol.asyncIterator](): AsyncIterator<T> }
```

`for await...of` consumes it, inside an `async` function or at the top level of a module:

```ts
for await (const page of fetchPages(url)) {
  render(page);
}
```

`for await` accepts sync iterables too (and awaits each value), so a parameter typed
`AsyncIterable<T> | Iterable<T>` takes arrays in tests and streams in production.

## Async generators

`async function*` combines both: `await` inside, `yield` to hand values out. The type is
`AsyncGenerator<T, TReturn, TNext>`:

```ts
async function* fetchPages(url: string, signal?: AbortSignal): AsyncGenerator<Page, void, undefined> {
  let next: string | null = url;
  while (next) {
    const response = await fetch(next, { signal });
    const page = PageSchema.parse(await response.json());   // validate at the boundary
    yield page;
    next = page.nextUrl;
  }
}
```

Each page is fetched only when the consumer asks for it: a consumer that stops after two
pages causes two requests, not twenty.

## Node.js sources that are async iterables

| Source | Yields |
| --- | --- |
| `fs.createReadStream(path)`, any `Readable` | Chunks: `Buffer` (or `string` with an encoding) |
| `readline.createInterface({ input, crlfDelay: Infinity })` | Lines of a stream |
| `events.on(emitter, "data", { signal })` | Each event's arguments, as an array |
| `setInterval(ms)` from `node:timers/promises` | A tick every `ms` |
| `(await fetch(url)).body` | `Uint8Array` chunks of the response |
| `fs.promises.opendir(dir)` | Directory entries |

`Readable.from(asyncIterable)` turns one back into a stream, and `stream.pipeline` connects
streams and async generators, propagating errors and cancellation:

```ts
await pipeline(
  createReadStream("access.log"),
  async function* (chunks) { for await (const chunk of chunks) yield transform(chunk); },
  createWriteStream("out.log"),
  { signal },
);
```

### Chunks are not lines

A chunk ends wherever the buffer ended: in the middle of a line, or even in the middle of a
multi-byte UTF-8 character. Keep the unfinished tail between chunks, and decode bytes with a
`TextDecoder` in **streaming** mode, which holds back an incomplete character until the next
chunk:

```ts
const decoder = new TextDecoder();
text += decoder.decode(chunk, { stream: true });   // "é" split over two chunks stays whole
// ... at the end:
text += decoder.decode();                          // flush what is left
```

## Backpressure

With `for await`, the producer runs only while the consumer asks: if writing to the database
is slow, reading the file slows down too, and memory stays flat. Two things break that:

- **Collecting everything**: `await Array.fromAsync(source)` keeps every item in memory. Fine
  for a test or a small source, wrong for a 10 GB log.
- **Push sources**: event emitters and `on("data")` callbacks do not wait for you; something
  must buffer. `events.on` buffers without limit, so prefer streams or async generators that
  pull.

## Early exit and cleanup

As with sync generators, `break`, `return` or an exception in a `for await` loop calls the
iterator's `return()`, which runs the async generator's `finally` blocks, **and awaits them**:

```ts
async function* lines(path: string): AsyncGenerator<string, void, undefined> {
  const file = await open(path);
  try {
    for await (const line of file.readLines()) yield line;
  } finally {
    await file.close();     // runs when the consumer stops early, too
  }
}
```

Stopping a pipeline at the end closes every stage up to the source: a consumer that finds what
it wants in the first megabyte stops the file from being read further. For objects you own,
`await using` with `[Symbol.asyncDispose]` does the same at the end of a block.

An exception thrown by the producer comes out of `for await` in the consumer, where an
ordinary `try/catch` handles it.

## Cancellation

An async generator cannot be interrupted while it waits on a promise, so cancellation needs an
`AbortSignal` that reaches the work being awaited:

```ts
async function* poll(url: string, signal: AbortSignal) {
  for await (const _ of setInterval(5_000, undefined, { signal })) {   // rejects on abort
    yield await (await fetch(url, { signal })).json();
  }
}
```

Check `signal.throwIfAborted()` between steps, and pass the signal to everything that accepts
one (`fetch`, timers, `events.on`, `pipeline`).

## Concurrency without losing order

`for await` handles one item at a time. When each item needs slow work (an HTTP call per
line), you want a few in flight at once while keeping the results in order. The idea, which
you will implement in the checkpoint:

1. Pull items from the source and start `fn(item)` until `concurrency` promises are pending.
2. Await the **oldest** pending promise and yield its result; then pull and start one more.
3. On an error or an early exit, abort the work still running and close the source.

```ts
for await (const user of mapConcurrent(ids, (id, signal) => loadUser(id, signal), { concurrency: 4 })) {
  console.log(user.name);   // in the order of ids, never more than 4 requests at once
}
```

The pull model still holds: the helper never reads more than `concurrency` items ahead.

Two traps: a pending promise that rejects while nobody awaits it yet is an **unhandled
rejection** (attach a no-op `.catch` and keep the original promise to await later), and
`Promise.race` over the pending set gives you results in completion order, not source order.

## Summary

- `AsyncIterable<T>` and `AsyncGenerator<T, TReturn, TNext>` are the iteration protocol with
  promises; `for await` consumes both async and sync iterables.
- Streams, readline, `events.on`, timers and `fetch` bodies are async iterables.
- Chunks are not lines: keep the tail and decode with `{ stream: true }`.
- Pulling gives backpressure; collecting everything or push sources lose it.
- Early exit calls `return()` all the way up the pipeline and awaits the `finally` blocks.
- Cancel with an `AbortSignal` that reaches every awaited operation; limit concurrency with a
  pool of pending promises yielded in source order.

## Further reading

- MDN, `for await...of`: <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/for-await...of>
- MDN, `AsyncGenerator`: <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/AsyncGenerator>
- Node.js, "Streams" (consuming readable streams with async iterators): <https://nodejs.org/api/stream.html#readablesymbolasynciterator>
- Node.js, "Backpressuring in Streams": <https://nodejs.org/en/learn/modules/backpressuring-in-streams>
- MDN, `TextDecoder.decode` and the `stream` option: <https://developer.mozilla.org/en-US/docs/Web/API/TextDecoder/decode>
- MDN, `Array.fromAsync`: <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/fromAsync>
