---
from: csharp
status: draft
concept: "JIT vs AOT compilation"
from_way: "The JIT by default, ReadyToRun, and Native AOT publishing with trimming warnings."
native_way: "The JIT by default, the AOT cache, and GraalVM native image."
sensei_tip: ".NET Native AOT and GraalVM native image make the same trade: fast startup and low memory, in exchange for a closed world and trimming or reachability metadata."
---

| C# | Java 25 |
| --- | --- |
| PublishAot=true | native-image / spring-boot:build-image -Pnative |
| trimming warnings | reachability metadata |
| ReadyToRun | AOT cache |
| DynamicDependency attribute | @RegisterReflectionForBinding / hints |
