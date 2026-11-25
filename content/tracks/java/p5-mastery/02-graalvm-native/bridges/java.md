---
from: java
status: draft
concept: "JIT vs AOT compilation"
from_way: "A JVM that takes seconds to start and needs hundreds of megabytes to warm up, shipped with a JRE on every machine."
native_way: "GraalVM produces a native executable that starts in milliseconds and uses a fraction of the memory, at the cost of a closed world and long builds."
sensei_tip: "Native images are a good answer to cold starts in serverless functions; for a busy long-running service, measure before you switch."
---

| JVM habit | Native image |
| --- | --- |
| `Class.forName(name)` anywhere | Declare it in reachability metadata, or avoid it |
| Reading `getResource(...)` freely | List resources in `resource-config` / `resources` |
| Ship a JRE with the app | Ship one executable per OS |
| Profile with JFR and full tooling | Limited tooling; reproduce on the JVM first |
