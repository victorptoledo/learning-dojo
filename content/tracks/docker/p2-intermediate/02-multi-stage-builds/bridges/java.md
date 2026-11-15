---
from: java
status: draft
concept: "Separate build and runtime environments"
from_way: "You build with a JDK and Maven on a CI agent and copy the JAR to servers that only need a JRE."
native_way: "A multi-stage Dockerfile does both in one file: a maven or JDK stage builds the JAR, a JRE (or distroless java) stage runs it."
sensei_tip: "eclipse-temurin:25-jre is far smaller than the JDK image; for even smaller images build a custom runtime with jlink in the build stage and copy it into a slim base."
---

| Java | Multi-stage |
| --- | --- |
| CI agent with JDK and Maven | `FROM maven:3-eclipse-temurin-25 AS build` |
| the JAR artifact | `COPY --from=build /src/target/app.jar /app/app.jar` |
| a server with a JRE | `FROM eclipse-temurin:25-jre` or `gcr.io/distroless/java25-debian13` |
| `jlink` custom runtimes | a build stage producing a minimal JRE |
