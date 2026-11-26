---
from: csharp
status: draft
concept: "IAsyncEnumerable and await foreach"
from_way: "IAsyncEnumerable<T> with async iterators (yield return in async methods), await foreach, CancellationToken with [EnumeratorCancellation], DisposeAsync, System.Threading.Channels."
native_way: "AsyncIterable<T> with async function*, for await, AbortSignal passed explicitly, return() running finally, streams and readline as sources."
sensei_tip: "IAsyncEnumerable maps directly. Where C# threads a CancellationToken through WithCancellation, TypeScript passes an AbortSignal as an ordinary argument."
---

| C# | TypeScript |
| --- | --- |
| `IAsyncEnumerable<T>` | `AsyncIterable<T>` |
| `async IAsyncEnumerable<T> Read() { yield return x; }` | `async function* read(): AsyncGenerator<T, void, undefined> { yield x; }` |
| `await foreach (var x in Read())` | `for await (const x of read())` |
| `CancellationToken` | `AbortSignal` |
| `await enumerator.DisposeAsync()` | `await iterator.return()` |
| `await using` | `await using` |
| `Channel<T>` | An async generator, or a small queue for push sources |
