---
from: java
status: draft
concept: "The JVM after Java 8"
from_way: "PermGen errors, -XX:+UseConcMarkSweepGC, -Xmx tuned by hand inside containers that the JVM did not understand, and paid profilers."
native_way: "Metaspace, G1 by default and generational ZGC for low latency, container-aware memory with MaxRAMPercentage, compact object headers, and free JFR and Mission Control."
sensei_tip: "Before touching a GC flag, record 60 seconds of JFR under real load: the answer is usually in the allocation or hot-method view."
---

| Java 8 | Java 25 |
| --- | --- |
| PermGen (`-XX:MaxPermSize`) | Metaspace |
| CMS collector | G1 (default) or ZGC; CMS was removed in Java 14 |
| `-Xmx` guessed for the container | `-XX:MaxRAMPercentage=75` |
| Commercial Flight Recorder | Free JFR (`-XX:StartFlightRecording`) |
| Slow warm-up | AOT cache (`-XX:AOTCache`) |
