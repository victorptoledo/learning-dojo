---
from: java
status: draft
concept: "Run only what is needed, with only the rights it needs"
from_way: "A fat JAR started as root on a VM with every port open and actuator exposed."
native_way: "A non-root service user or distroless container, only the app port reachable, actuator on a separate management port, and patched base images."
sensei_tip: "Set `management.server.port` to a separate port bound to localhost or an internal network, and expose only `health` publicly: actuator endpoints like `env` and `heapdump` leak secrets."
---

| Java | Hardened deployment |
| --- | --- |
| java -jar app.jar as root | a systemd unit with User= and sandboxing |
| actuator on the public port | management.server.port internal |
| full JDK image | a JRE or distroless image |
| JMX open remotely | disabled or bound to localhost |
| manual patching | rebuilt images on a schedule |
