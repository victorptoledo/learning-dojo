---
from: python
status: draft
concept: "Least privilege for the process and the network"
from_way: "gunicorn runs as www-data on a VM, and security groups decide which hosts may talk."
native_way: "The security context runs the Pod as a non-root UID with no capabilities, and NetworkPolicies replace security groups between Pods."
sensei_tip: "Python writes .pyc files and temp files: with a read-only root, set PYTHONDONTWRITEBYTECODE=1 and mount an emptyDir at /tmp."
---

| Python world | Kubernetes |
| --- | --- |
| `user www-data` | `runAsUser` with a numeric UID |
| cloud security groups | NetworkPolicies |
| AppArmor profile on the host | seccomp `RuntimeDefault` (and AppArmor fields) |
| `chmod -R a-w /app` | `readOnlyRootFilesystem: true` |
