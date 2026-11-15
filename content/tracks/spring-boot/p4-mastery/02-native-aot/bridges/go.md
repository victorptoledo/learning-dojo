---
from: go
status: draft
concept: "Fast startup for Spring services"
from_way: "Instant startup from a native binary."
native_way: "A native image to get close to Go's startup, or the JVM tuned with the AOT cache."
sensei_tip: "If startup time is your only reason to consider Go, try a Spring native image first; if it is memory and simplicity, Go may still be the better fit."
---

| Go | Spring Boot 4 |
| --- | --- |
| go build | native-image via spring-boot:build-image -Pnative |
| ~10 ms startup | native image ~50-100 ms |
| small RSS | native image RSS |
