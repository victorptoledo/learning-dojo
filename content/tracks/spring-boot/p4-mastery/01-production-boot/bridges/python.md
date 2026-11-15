---
from: python
status: draft
concept: "Shipping a Spring Boot service"
from_way: "gunicorn behind nginx, environment configuration, Docker images and Alembic migrations at deploy."
native_way: "A layered or buildpack image, configuration from the environment, Flyway at startup, probes and graceful shutdown."
sensei_tip: "Spring Boot's server.shutdown=graceful is gunicorn's graceful timeout: in-flight requests finish after SIGTERM. Readiness and liveness come from Actuator."
---

| Python | Spring Boot 4 |
| --- | --- |
| gunicorn --graceful-timeout | server.shutdown=graceful |
| Dockerfile with pip install | spring-boot:build-image or a layered JAR |
| alembic upgrade head | Flyway on startup |
| /health | /actuator/health/liveness and readiness |
