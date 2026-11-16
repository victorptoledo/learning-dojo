---
from: csharp
status: draft
concept: "A runtime with async I/O at its core"
from_way: ".NET runs IL on the CLR with a JIT; async/await frees thread-pool threads during I/O, and ASP.NET Core serves requests on many threads."
native_way: "Node.js runs JavaScript on V8 with a JIT; async/await frees the single main thread during I/O, and one process serves many requests on that thread."
sensei_tip: "async/await looks the same as in C#, but there is no thread pool running your continuations in parallel: they all run one after another on the main thread, so CPU work in a handler hurts every request."
---

| C# / .NET | Node.js |
| --- | --- |
| CLR with JIT and GC | V8 with JIT and GC |
| Thread pool runs continuations | One thread runs every callback |
| `dotnet run` | `node app.ts` |
| LTS releases (8, 10) | LTS releases (even majors) |
| `Task.Run` for CPU work | worker threads |
