---
from: javascript
status: draft
concept: "Operating the platform, not only the app"
from_way: "A PaaS hides upgrades, backups and policies, and deploys on every push."
native_way: "On Kubernetes you (or your platform team) own them: upgrades within the skew policy, backups, policy engines, and GitOps to keep push-to-deploy."
sensei_tip: "A platform team's job is to give developers the PaaS experience back: templates, GitOps, preview environments, and guard rails as policies."
---

| PaaS habit | Kubernetes |
| --- | --- |
| push to deploy | CI updates Git, GitOps applies |
| instant rollback | a Git revert, or Argo Rollouts |
| the platform's status page | your own SLOs and alerts |
| the provider's backups | your etcd/GitOps and data backups |
