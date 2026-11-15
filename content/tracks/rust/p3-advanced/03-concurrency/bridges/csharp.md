---
from: csharp
status: draft
concept: "Multithreading and synchronisation"
from_way: "Threads, Task.Run, lock, Concurrent collections and Parallel.ForEach."
native_way: "Threads with Send and Sync checks, Mutex owning its data, and channels."
sensei_tip: "In C# the lock and the data it protects are separate; in Rust the Mutex owns the data, so you cannot touch it without holding the lock."
---

| C# | Rust |
| --- | --- |
| Task.Run(() => ...) | thread::spawn(\|\| ...) |
| lock (obj) | mutex.lock() |
| ConcurrentDictionary | Mutex<HashMap> or dashmap |
| Channel<T> | mpsc::channel |
| Parallel.ForEach | rayon par_iter |
