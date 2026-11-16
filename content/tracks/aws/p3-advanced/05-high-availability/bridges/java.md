---
from: java
status: draft
concept: "Design for failure: spread, detect, replace, recover"
from_way: "An active-passive app server pair with a shared database and a disaster recovery document nobody tested."
native_way: "Multi-AZ fleets behind load balancers, replicated data, Route 53 failover and rehearsed DR in a second region."
sensei_tip: "Make Spring Boot health endpoints honest (`/actuator/health/readiness` checks dependencies), and set the AWS SDK retry mode to `standard` or `adaptive` with timeouts so a failing AZ does not hang threads."
---

| Java world | AWS |
| --- | --- |
| an active-passive cluster | Auto Scaling across AZs behind an ALB |
| Oracle Data Guard | Aurora Global Database |
| a DR runbook in Word | infrastructure as code plus game days |
| Resilience4j circuit breakers | the same, plus Route 53 health checks |
