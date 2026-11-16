---
from: csharp
status: draft
concept: "Async I/O"
from_way: "async/await with Task, HttpClient and CancellationToken."
native_way: "async/await with Promise, fetch and AbortSignal; the UI thread rule from WPF applies: never block it."
sensei_tip: "CancellationTokenSource is AbortController, and EnsureSuccessStatusCode is the response.ok check you must write."
---

| C# | In the browser |
| --- | --- |
| `await httpClient.GetAsync(url, token)` | `await fetch(url, { signal })` |
| `response.EnsureSuccessStatusCode()` | `if (!response.ok) throw ...` |
| `Task.WhenAll(a, b)` | `Promise.all([a, b])` |
| `CancellationTokenSource.Cancel()` | `controller.abort()` |
