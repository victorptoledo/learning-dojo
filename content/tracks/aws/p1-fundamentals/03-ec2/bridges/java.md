---
from: java
status: draft
concept: "Instances are disposable; build them from images and give them roles"
from_way: "A Tomcat server is hand-built once, tuned for years and copied by snapshot when it breaks."
native_way: "Instances come from an AMI and user data through a launch template; an Auto Scaling group replaces them; credentials come from the instance profile."
sensei_tip: "On Graviton, use an Arm build of Corretto 21 or 25 and set `-XX:MaxRAMPercentage` instead of a fixed `-Xmx`, so one AMI fits several instance sizes."
---

| Java world | AWS |
| --- | --- |
| a pet server | an Auto Scaling group of identical instances |
| `-Xmx` tuned per box | `-XX:MaxRAMPercentage` on a right-sized instance |
| an init.d script | user data plus a systemd unit |
| SSH to read logs | Session Manager and CloudWatch Logs |
