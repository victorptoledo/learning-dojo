---
from: csharp
status: draft
concept: "Fast startup for Spring services"
from_way: "ReadyToRun and Native AOT with trimming for fast startup."
native_way: "The AOT cache, CRaC and GraalVM native images."
sensei_tip: "Spring AOT processing generates code at build time like .NET source generators do, so the native image needs no runtime reflection for your beans."
---

| C# | Spring Boot 4 |
| --- | --- |
| Native AOT | native image |
| ReadyToRun | AOT cache |
| source generators | Spring AOT processing |
| trimming warnings | reachability hints |
