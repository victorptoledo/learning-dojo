---
from: java
status: draft
concept: "Operating a Python service like a JVM service"
from_way: "SLF4J with Logback and JSON encoders, Spring profiles and environment variables, a fat JAR or jlink image, one JVM using every core with thread pools, Actuator health checks, and OWASP dependency-check."
native_way: "logging with module loggers and a JSON formatter, pydantic-settings from the environment, an image with a locked .venv built by uv, several worker processes, health endpoints, OpenTelemetry and pip-audit."
sensei_tip: "The biggest operational difference: one JVM uses every core, while a standard Python service needs several processes (workers or replicas) to do the same. Size memory per process accordingly."
---

| Java | Python |
| --- | --- |
| `LoggerFactory.getLogger(X.class)` | `logging.getLogger(__name__)` |
| `log.error("failed", e)` | `log.exception("failed")` |
| Logback JSON encoder | `structlog` or a JSON formatter |
| `@ConfigurationProperties` + env vars | `pydantic_settings.BaseSettings` |
| fat JAR / jlink image | image with the `.venv` from `uv sync --locked --no-dev` |
| one JVM, many threads | several worker processes |
| Actuator `/health` | a health endpoint |
| OWASP dependency-check | `pip-audit` |
