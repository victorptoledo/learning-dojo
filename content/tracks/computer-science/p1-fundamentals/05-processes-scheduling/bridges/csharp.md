---
from: csharp
status: draft
concept: "Processes and threads in C#"
from_way: "Creating Threads by hand for every task."
native_way: "Task and async/await run work on the thread pool, which grows and shrinks threads by itself; a Thread is an OS thread; Process.Start launches a process."
sensei_tip: "Use async/await for I/O and Parallel.For or Task.Run for CPU work; avoid blocking pool threads with .Result, which starves the pool."
---

| In C# | Underneath |
| --- | --- |
| `Task.Run` | a work item on pool threads |
| `async`/`await` | a state machine resumed by the pool |
| `new Thread()` | an OS thread |
| `Process.Start` | a child process |
| `ThreadPool.SetMinThreads` | how fast the pool adds threads |
