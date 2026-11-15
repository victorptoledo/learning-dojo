---
from: java
status: draft
concept: "Running services in production"
from_way: "Blue-green deployments of WARs on application servers, Spring profiles per environment, and Actuator health endpoints for the load balancer."
native_way: "One image for every environment, configured by environment variables; Actuator liveness and readiness groups map directly onto orchestrator probes, and graceful shutdown drains on SIGTERM."
sensei_tip: "Spring Boot exposes /actuator/health/liveness and /readiness when it detects Kubernetes (or with management.endpoint.health.probes.enabled=true); readiness goes down during graceful shutdown."
---

| Java / Spring | Production containers |
| --- | --- |
| a WAR per environment | one image, configured by environment |
| Actuator health | liveness and readiness probes |
| `server.shutdown=graceful` | draining on SIGTERM within the grace period |
| Flyway or Liquibase migrations | backward compatible, run before or with the rollout |
