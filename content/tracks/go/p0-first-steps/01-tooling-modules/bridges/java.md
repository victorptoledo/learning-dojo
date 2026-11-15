---
from: java
status: draft
concept: "Build tool, dependencies and packaging"
from_way: "Maven or Gradle with a pom.xml or build.gradle, a JDK to install on every server, plugins for formatting and checks, and a fat JAR or a container with a JVM."
native_way: "The go command is the build tool, test runner, formatter and dependency manager; go.mod and go.sum describe the module, and go build produces one static binary."
sensei_tip: "Resist adding Make targets or wrappers on day one: go build, go test ./... and go vet ./... are the whole build for most services."
---

| Java | Go |
| --- | --- |
| `pom.xml` / `build.gradle` | `go.mod` |
| Maven Central, `~/.m2` | module proxy (`proxy.golang.org`), module cache |
| `groupId:artifactId:version` | module path `github.com/acme/lib` and a semver tag |
| `mvn package` → JAR + a JRE | `go build` → one native binary |
| `mvn test` | `go test ./...` |
| package-private by default, `public` keyword | lower case = package-private, Upper case = exported |
| Checkstyle, Spotless, google-java-format | `gofmt` (no options to argue about) |
| `module-info.java` exports | `internal/` folders |
