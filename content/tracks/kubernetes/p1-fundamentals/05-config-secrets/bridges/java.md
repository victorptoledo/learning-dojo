---
from: java
status: draft
concept: "Configuration outside the artifact"
from_way: "application-prod.properties packaged in the JAR, or a Spring Cloud Config server, with passwords in a Jenkins credential."
native_way: "ConfigMaps and Secrets hold the values; Spring reads them as environment variables or as files mounted into the container."
sensei_tip: "Mount a ConfigMap as /config/application.yaml and set spring.config.import=optional:configtree:/etc/secrets/ to read each Secret key as a property."
---

| Spring world | Kubernetes |
| --- | --- |
| `application-prod.yaml` | a ConfigMap mounted as a file |
| `SPRING_DATASOURCE_PASSWORD` env | `secretKeyRef` |
| Spring Cloud Config refresh | a mounted file that updates, or a rollout |
| Jasypt-encrypted properties | Sealed Secrets or SOPS in Git |
