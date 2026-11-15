---
from: java
status: draft
concept: "Describing a multi-service environment"
from_way: "Spring profiles and application-local.yml, plus a README listing the databases to install, or Testcontainers starting them from tests."
native_way: "compose.yaml declares the API, the database and the cache with their configuration, and docker compose up starts the whole environment the same way for everyone."
sensei_tip: "Spring Boot's spring-boot-docker-compose module starts compose.yaml when the app starts in development and configures the connection details for you."
---

| Java / Spring | Compose |
| --- | --- |
| `application-local.yml` | `environment:` and `.env` values |
| Spring profiles | Compose profiles and override files |
| Testcontainers in tests | `docker compose up` for local and CI environments |
| `spring.datasource.password` in a file | a secret mounted at `/run/secrets/...` |
