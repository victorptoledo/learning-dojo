---
from: go
status: draft
concept: "Shipping a Spring Boot service"
from_way: "A static binary in a small image, configuration from the environment, and migrations as a step."
native_way: "A JVM image built by buildpacks or layers, with the same environment-driven configuration."
sensei_tip: "The JVM image is bigger and slower to start than a Go binary; layered JARs keep rebuilds small, and CDS or the AOT cache cut startup time."
---

| Go | Spring Boot 4 |
| --- | --- |
| FROM scratch + binary | buildpack image with a JRE |
| srv.Shutdown(ctx) | server.shutdown=graceful |
| golang-migrate | Flyway |
| /healthz | Actuator probes |
