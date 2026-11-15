---
from: java
status: draft
concept: "Least privilege for the process and the network"
from_way: "The application server runs as a service user on a host behind a firewall, and every service in the data centre can reach every other."
native_way: "Each container runs non-root with no capabilities and a read-only file system, and NetworkPolicies allow only the calls the service really makes."
sensei_tip: "With readOnlyRootFilesystem the JVM still needs a writable /tmp (and Tomcat a work directory): mount an emptyDir there, and set -Djava.io.tmpdir if you use another path."
---

| Java world | Kubernetes |
| --- | --- |
| a `tomcat` service user | `runAsNonRoot` + `runAsUser` |
| the Java Security Manager (removed) | seccomp, capabilities, no privilege escalation |
| firewall rules between tiers | NetworkPolicies |
| a hardened OS baseline | the restricted Pod Security Standard |
