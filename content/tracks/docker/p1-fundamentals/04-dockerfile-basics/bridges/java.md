---
from: java
status: draft
concept: "A declarative build recipe"
from_way: "A pom.xml or build.gradle describes how to compile and package; the result is a JAR, and how it runs is decided elsewhere (scripts, a service manager)."
native_way: "A Dockerfile describes both: how to assemble the file system and how the result starts (ENTRYPOINT, CMD, USER, ENV, EXPOSE)."
sensei_tip: "Keep the Maven or Gradle build in the Dockerfile's first stage and copy only the JAR into a JRE image; never COPY a target/ folder built on your laptop."
---

| Java | Dockerfile |
| --- | --- |
| `<parent>` POM | `FROM` |
| a build plugin execution | `RUN` |
| resources copied into the JAR | `COPY` |
| `Main-Class` in the manifest | `ENTRYPOINT ["java", "-jar", "app.jar"]` |
| `-Dprop=value` defaults | `ENV` or `CMD` defaults |
