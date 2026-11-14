---
from: java
status: draft
concept: "Shipping a Spring Boot service"
from_way: "A WAR copied to an application server, schema changed by hand or by ddl-auto=update, one build per environment, and kill -9 to stop it."
native_way: "A layered executable JAR in a non-root container image, Flyway migrations with ddl-auto=validate, one image promoted everywhere, probes and graceful shutdown."
sensei_tip: "Practise a rollback before you need one: deploy the previous image and check that the database migrations still fit it."
---

| Before | Now |
| --- | --- |
| WAR in Tomcat | `java -jar` or a container image |
| `ddl-auto=update` in production | Flyway + `ddl-auto=validate` |
| A build per environment | One image, configuration from the environment |
| `kill -9` | `SIGTERM` + graceful shutdown |
| Fixed Tomcat thread pool tuning | `spring.threads.virtual.enabled=true` |
