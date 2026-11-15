---
from: java
status: draft
concept: "One definition, many environments"
from_way: "Maven profiles and Spring profiles switch configuration per environment, and a build produces a different artifact per profile."
native_way: "Kustomize overlays (or Helm values files) vary the deployment per environment while the image stays the same."
sensei_tip: "Think of a Helm chart as a Maven artifact: versioned, published to a registry, with a values file as its configuration; Kustomize overlays are closer to Spring profiles."
---

| Java world | Kubernetes |
| --- | --- |
| a parent POM | a Kustomize base |
| a Maven profile `prod` | an overlay `overlays/prod` |
| an artifact in Nexus | a chart in an OCI registry |
| `application-prod.yaml` | a values file or a configMapGenerator |
