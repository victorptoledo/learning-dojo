---
from: python
status: draft
concept: "Operating the platform, not only the app"
from_way: "Ansible playbooks configure servers, and an upgrade of the OS is a project every few years."
native_way: "The cluster itself is upgraded several times a year, workloads are declared in Git and applied by Argo CD or Flux, and backups and policies are part of the platform."
sensei_tip: "Write policy tests for your manifests like unit tests: render them in CI and check them, as this dojo's katas do with kubeconform and bats."
---

| Python world | Kubernetes |
| --- | --- |
| Ansible as the source of truth | Git with GitOps |
| `pip list --outdated` | add-on and version reviews |
| a DB dump cron | volume snapshots, Velero, database backups |
| Sentry alerts | SLO alerts from metrics and traces |
