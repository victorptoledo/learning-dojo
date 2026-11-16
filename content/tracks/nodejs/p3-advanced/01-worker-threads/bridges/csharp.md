---
from: csharp
status: draft
concept: "Offloading CPU work"
from_way: "Task.Run or Parallel.For put CPU work on thread-pool threads that share the heap, with locks or concurrent collections for shared data."
native_way: "CPU work goes to worker threads with separate heaps, called through a pool; data travels as messages, transferred or shared only through SharedArrayBuffer."
sensei_tip: "There is no Task.Run in Node.js: the thread pool is libuv's and runs native work, not your JavaScript. For your own CPU work, a worker pool is the equivalent."
---

| C# | Node.js |
| --- | --- |
| `await Task.Run(() => Resize(img))` | `await pool.run({ img })` |
| `Parallel.ForEach` | Split the work over the pool's workers |
| `Channel<T>` | `MessagePort` |
| `Interlocked.Increment` | `Atomics.add` |
| `Environment.ProcessorCount` | `os.availableParallelism()` |
