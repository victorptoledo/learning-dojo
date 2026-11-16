---
from: java
status: draft
concept: "Small, reversible changes; practised incidents; blameless learning"
from_way: "Quarterly releases in a change window, and war rooms when they fail."
native_way: "Pipelines deploy small changes progressively with automatic rollback; incidents follow roles and runbooks."
sensei_tip: "Ship Spring Boot services through canary deployments with CodeDeploy or ECS blue/green, and gate each step on actuator-backed health and CloudWatch alarms."
---

| Java world | AWS |
| --- | --- |
| a change advisory board | progressive delivery with alarms |
| a release weekend | many small deployments a day |
| an ops runbook PDF | Systems Manager Automation runbooks |
| a war room | Incident Manager with roles and a timeline |
