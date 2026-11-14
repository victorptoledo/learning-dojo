---
from: java
status: draft
concept: "Fast startup for Spring services"
from_way: "Seconds of startup and a long warm-up accepted as the price of Spring, or Spring Native experiments with many workarounds."
native_way: "Spring AOT built into Boot, GraalVM native images with generated metadata, the JVM AOT cache from a training run, and CRaC snapshots."
sensei_tip: "Start with the JVM AOT cache: it is one flag, keeps your tooling, and often gives most of the startup win you need."
---

| Before | Now |
| --- | --- |
| Spring Native (experimental) | AOT processing and native support built into Spring Boot |
| Reflection anywhere | `@RegisterReflectionForBinding`, `RuntimeHintsRegistrar` |
| Class Data Sharing archives by hand | `-XX:AOTCacheOutput` / `-XX:AOTCache` |
| Warm-up scripts | CRaC checkpoint and restore |
