---
from: java
status: draft
concept: "Declared state instead of start scripts"
from_way: "An application server or a systemd unit starts your JAR, and a runbook says what to do when the machine dies."
native_way: "A Deployment declares how many copies of the image should run; controllers start, restart and move them for you."
sensei_tip: "Treat the container as the unit you ship, like the JAR once was: build it once in CI and promote the same image through every environment."
---

| Java world | Kubernetes |
| --- | --- |
| `java -jar app.jar` on a server | a container in a Pod |
| a systemd unit with `Restart=always` | a Deployment (the kubelet restarts, controllers replace) |
| a load balancer in front of three servers | a Service over three replicas |
| `application.properties` per environment | a ConfigMap per environment |
