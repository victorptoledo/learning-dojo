---
from: java
status: draft
concept: "Packaging, deployment and dependency security"
from_way: "Fat JARs on a JRE base image, jlink or GraalVM native-image for small images, Spring profiles and application.yml, OWASP dependency-check or Snyk, and JVM flags tuned per container."
native_way: "A static binary on a distroless image by default, configuration from the environment, govulncheck with call-graph analysis, and a runtime that reads container limits by itself."
sensei_tip: "What GraalVM native-image gives Java after a lot of configuration, go build gives you by default: one native binary that starts in milliseconds."
---

| Java | Go |
| --- | --- |
| fat JAR + `eclipse-temurin` image | static binary + distroless image |
| GraalVM native-image | `CGO_ENABLED=0 go build` |
| `application-prod.yml` profiles | environment variables into a config struct |
| `@Value("${db.url}")` | `os.Getenv("DATABASE_URL")` at start-up |
| OWASP dependency-check | `govulncheck ./...` |
| `-XX:MaxRAMPercentage` | `GOMEMLIMIT` |
| Maven Enforcer / checksums | `go.sum` and the checksum database |
| OpenRewrite migrations | `go fix` modernizers |
