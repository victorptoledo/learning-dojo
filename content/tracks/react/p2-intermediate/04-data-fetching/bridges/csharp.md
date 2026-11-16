---
from: csharp
status: draft
concept: "async with CancellationToken"
from_way: "You await HttpClient.GetFromJsonAsync with a CancellationToken; Blazor cancels it in Dispose; EnsureSuccessStatusCode throws on errors."
native_way: "fetch takes an AbortSignal (the CancellationToken), the effect cleanup aborts it, and you check response.ok instead of EnsureSuccessStatusCode."
sensei_tip: "AbortController is CancellationTokenSource, signal is the token, abort() is Cancel(). Pass it everywhere, exactly as you would in .NET."
---

| C# | React |
| --- | --- |
| `CancellationTokenSource` | `AbortController` |
| `token` | `controller.signal` |
| `cts.Cancel()` in `Dispose` | `controller.abort()` in the cleanup |
| `EnsureSuccessStatusCode()` | `if (!response.ok) throw ...` |
| `IMemoryCache` | TanStack Query cache |
| `Task.WhenAll` | `Promise.all` |
