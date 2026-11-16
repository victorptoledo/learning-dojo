---
from: java
status: reviewed
concept: "Modules before microservices on the JVM"
from_way: "One Spring Boot service per entity, each with its own repository and database, deployed together anyway."
native_way: "A modular monolith with Spring Modulith or ArchUnit-enforced packages, extracted into services only for concrete forces, with a BFF for clients that need their own shapes."
sensei_tip: "Spring Modulith verifies module boundaries in a test and can publish events between modules, which makes a later extraction much easier."
---

| Java | Architecture |
| --- | --- |
| Spring Modulith `ApplicationModules.verify()` | enforced module boundaries |
| ArchUnit rules | no cross-module internals |
| Spring Cloud Gateway | an edge gateway |
| Gradle multi-project | a JVM monorepo |
