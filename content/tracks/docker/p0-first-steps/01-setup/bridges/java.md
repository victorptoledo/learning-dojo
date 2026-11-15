---
from: java
status: draft
concept: "Packaging an application with its runtime"
from_way: "You ship a JAR and hope the server has the right JDK, system libraries and environment; application servers and fat JARs reduce the problem but do not remove it."
native_way: "An image carries the JRE, the JAR, the configuration defaults and the start command, so the server only needs a container runtime."
sensei_tip: "The JVM has respected container memory and CPU limits since JDK 10, so size the heap with -XX:MaxRAMPercentage instead of a fixed -Xmx."
---

| Java | Docker |
| --- | --- |
| a JAR plus "install JDK 25 on the server" | an image `FROM eclipse-temurin:25-jre` with the JAR inside |
| `java -jar app.jar` in a start script | `ENTRYPOINT ["java", "-jar", "/app/app.jar"]` |
| Maven Central | a registry such as Docker Hub or ghcr.io |
| one JVM per VM | many containers per host, each with its own limits |
