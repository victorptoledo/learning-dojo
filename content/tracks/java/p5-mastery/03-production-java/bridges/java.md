---
from: java
status: draft
concept: "Shipping Java today"
from_way: "WAR files deployed into an application server, log4j 1.x with string concatenation, properties files per environment baked into the build, and System.out for debugging."
native_way: "A runnable JAR in a container image, SLF4J with placeholders and JSON output, configuration from the environment validated at startup, and OpenTelemetry for traces."
sensei_tip: "If you need to rebuild to change a setting between environments, that setting belongs in the environment."
---

| Then | Now |
| --- | --- |
| WAR in Tomcat or JBoss | Runnable JAR in a container |
| `log.info("x=" + x)` | `log.info("x={}", x)` |
| `config-prod.properties` in the build | Environment variables, validated at startup |
| `catch (Exception e) { e.printStackTrace(); }` | `log.error("...", e)` once, where it is handled |
| Full JRE in the image | `eclipse-temurin:25-jre` or a `jlink` runtime |
