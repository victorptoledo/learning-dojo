---
from: csharp
status: draft
concept: "Channels, Task.WhenAny and SemaphoreSlim"
from_way: "System.Threading.Channels with Writer.Complete and ReadAllAsync, Task.WhenAny for the first result or a timeout, SemaphoreSlim to limit concurrency and BlockingCollection."
native_way: "Go channels are the language feature that System.Threading.Channels copied: close replaces Complete, range replaces ReadAllAsync, select replaces WhenAny, and a buffered channel replaces SemaphoreSlim."
sensei_tip: "Channel.CreateBounded(10) is make(chan T, 10). The big extra in Go is select, which waits on sends and receives of many channels at once."
---

| C# | Go |
| --- | --- |
| `Channel.CreateUnbounded<T>()` | no equivalent: Go channels are always bounded |
| `Channel.CreateBounded<T>(10)` | `make(chan T, 10)` |
| `await writer.WriteAsync(x)` | `ch <- x` |
| `writer.Complete()` | `close(ch)` |
| `await foreach (var x in reader.ReadAllAsync())` | `for x := range ch` |
| `await Task.WhenAny(a, b)` | `select` |
| `new SemaphoreSlim(3)` | `make(chan struct{}, 3)` |
| `reader.TryRead(out var x)` | `select` with `default` |
