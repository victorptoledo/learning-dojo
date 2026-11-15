---
from: java
status: draft
concept: "Incremental builds and dependency caching"
from_way: "Maven and Gradle cache dependencies in ~/.m2 or ~/.gradle and recompile only what changed."
native_way: "A Docker build starts clean every time except for its layer cache, so copy pom.xml first, resolve dependencies in their own layer, then copy src/."
sensei_tip: "mvn dependency:go-offline after COPY pom.xml makes the dependency layer cacheable; with Gradle, copy the build files and run gradle dependencies first."
---

| Java build | Dockerfile |
| --- | --- |
| `~/.m2` cache | a cached layer after `COPY pom.xml` + resolve |
| incremental compilation | the layer cache, in instruction order |
| `<version>` pinned in the POM | `FROM eclipse-temurin:25-jre` plus a digest |
| Checkstyle | hadolint |
