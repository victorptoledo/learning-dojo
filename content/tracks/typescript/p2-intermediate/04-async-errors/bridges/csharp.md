---
from: csharp
status: draft
concept: "Tasks, exceptions and cancellation tokens"
from_way: "async Task<T> methods, Task.WhenAll, exceptions caught by type, CancellationToken passed explicitly, IDisposable with using."
native_way: "async functions returning Promise<T>, Promise.all, catch with unknown, AbortSignal passed explicitly, using with Symbol.dispose."
sensei_tip: "async/await came to JavaScript from C#, so the shape is familiar. Two differences: a promise starts running when it is created (no cold tasks), and you cannot catch by type, only narrow inside one catch."
---

| C# | TypeScript |
| --- | --- |
| `async Task<User> LoadAsync()` | `async function load(): Promise<User>` |
| `await Task.WhenAll(a, b)` | `await Promise.all([a, b])` |
| `catch (HttpRequestException e)` | `catch (e) { if (e instanceof HttpError) ... }` |
| `CancellationToken` | `AbortSignal` |
| `using var conn = ...` | `using conn = ...` |
| `AggregateException` | `AggregateError` |
