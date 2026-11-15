---
from: go
status: draft
concept: "Getting started with Java 25"
from_way: "go run, go build and modules, producing one native binary."
native_way: "javac plus the JVM, Maven or Gradle, and a JAR that needs a JDK or a container to run."
sensei_tip: "A JAR is not a single binary: it needs a JVM. For Go-like deployment, use a container image with the JRE, or GraalVM native image (Phase 4)."
---

| Go | Java 25 |
| --- | --- |
| go run . | java App.java |
| go build | mvn package (a JAR) |
| go.mod | pom.xml |
| gofmt | a formatter plugin (Spotless) |
| static binary | JAR + JVM, or a native image |
