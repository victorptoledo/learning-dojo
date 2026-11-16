---
from: csharp
status: draft
concept: "Tasks and promises"
from_way: "async Task<T>, Task.WhenAll and WhenAny, CancellationToken passed down explicitly, and unobserved task exceptions that are usually ignored."
native_way: "async functions returning Promise<T>, Promise.all and race, AbortSignal passed down explicitly, and unhandled rejections that end the process."
sensei_tip: "The model is the one C# made popular, so it will feel natural. Two traps: promises are hot (they run when created, like started tasks), and an ignored rejection is fatal, not unobserved."
---

| C# | Node.js |
| --- | --- |
| `Task<T>` | `Promise<T>` |
| `Task.WhenAll` | `Promise.all` |
| `Task.WhenAny` | `Promise.race` |
| `CancellationToken` | `AbortSignal` |
| `IAsyncEnumerable<T>` + `await foreach` | Async iterable + `for await` |
| `TaskScheduler.UnobservedTaskException` | `process.on("unhandledRejection")` |
