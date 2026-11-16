---
from: java
status: draft
concept: "Developers own their service in production"
from_way: "You build a WAR or a fat JAR, hand it to an ops team with a deployment guide, and they install it on the application servers."
native_way: "Your team builds an image in CI, deploys it through a pipeline you wrote, watches its dashboards and joins the on-call rota."
sensei_tip: "Learn to read a JVM in production: GC logs, heap dumps, thread dumps and the actuator metrics you would ask ops for today."
---

| Java world | DevOps |
| --- | --- |
| `mvn package` on your laptop | the same build in CI on every commit |
| a deployment guide for ops | a pipeline and a runbook in the repository |
| the ops team's JMX console | Prometheus metrics from Micrometer |
| release weekend | many small deploys a day |
