---
from: java
status: draft
concept: "A task definition is the Pod; a service keeps it running"
from_way: "Jars are deployed to app servers, or images are run with docker compose on one VM."
native_way: "Images in ECR, task definitions with roles and logs, services behind an ALB on Fargate or EKS."
sensei_tip: "Set `-XX:MaxRAMPercentage=75` so the JVM sizes its heap from the task memory, and give Spring Boot's shutdown time to finish before ECS's `stopTimeout`."
---

| Java world | AWS |
| --- | --- |
| a Jib-built image | an image in ECR |
| `docker compose up` | an ECS service per container |
| actuator health endpoint | ALB target group health check |
| `application-prod.yml` secrets | task definition `secrets` from Secrets Manager |
