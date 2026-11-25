---
from: go
status: draft
concept: "JIT vs AOT compilation"
from_way: "Always compiled ahead of time to a native binary."
native_way: "JIT by default, native image when startup and memory matter more than peak throughput."
sensei_tip: "A native image gives Java Go-like deployment, but without the JIT's profile-guided optimisation; for long-running busy services the JVM is often faster."
---

| Go | Java 25 |
| --- | --- |
| go build | native-image |
| static binary | native executable (mostly static) |
| reflection limits | closed-world analysis |
| PGO (go build -pgo) | the JIT, or native image PGO |
