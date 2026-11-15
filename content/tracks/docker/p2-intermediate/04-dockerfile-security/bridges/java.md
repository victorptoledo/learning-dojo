---
from: java
status: draft
concept: "Least privilege and a minimal attack surface"
from_way: "Running Tomcat as an unprivileged service account and removing the manager app and sample webapps from production servers."
native_way: "Strip the image to the JRE and your JAR, switch to a numeric non-root user, and make the application files read-only for that user."
sensei_tip: "Distroless java images (gcr.io/distroless/java25-debian13:nonroot) run as UID 65532 and have no shell; use jcmd from a debug container when you need diagnostics."
---

| Java habit | Image hardening |
| --- | --- |
| a `tomcat` service account | `USER 10001:10001` |
| deleting sample webapps | no build tools or extras in the final stage |
| Spring Actuator `/actuator/health` | the target of the health check |
| a JRE-only server | a JRE or distroless java runtime stage |
