---
from: go
status: draft
concept: "Shipping Java today"
from_way: "log/slog, env config and a static binary in a scratch image."
native_way: "SLF4J, environment config and a JRE image with the JAR."
sensei_tip: "The JVM image is bigger than a Go one; jlink can build a trimmed runtime with only the modules you use, and a native image closes the gap further."
---

| Go | Java 25 |
| --- | --- |
| log/slog | SLF4J + Logback (JSON encoder) |
| os.Getenv | System.getenv / framework config |
| FROM scratch | a jlink runtime or distroless java |
| expvar / prometheus | Micrometer |
