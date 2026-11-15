---
from: javascript
status: draft
concept: "Shipping a Spring Boot service"
from_way: "A node image, environment variables, migrations in a release step and process managers."
native_way: "One container image, environment config, Flyway and Actuator probes."
sensei_tip: "Node needs custom SIGTERM handling to stop gracefully; Spring Boot does it with one property, and its probes plug straight into Kubernetes."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| node:22 image | buildpack image (paketo) |
| server.close() on SIGTERM | server.shutdown=graceful |
| knex migrate:latest | Flyway |
| NODE_ENV=production | spring.profiles.active=prod |
