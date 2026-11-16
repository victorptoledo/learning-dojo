---
from: java
status: draft
concept: "Promote one artefact; let the environment pull its state"
from_way: "A release manager deploys a version from Nexus to each environment with a Jenkins job and a properties file per environment."
native_way: "CI pushes one image; each environment's configuration in Git names its version, and Argo CD applies it."
sensei_tip: "Spring profiles per environment become Kustomize overlays or Helm values files, reviewed in pull requests."
---

| Java world | GitOps |
| --- | --- |
| a version in Nexus | an image tag in the registry |
| `application-prod.properties` | the prod overlay in the config repo |
| a Jenkins deploy job | an Argo CD Application |
| redeploying the old WAR | `git revert` |
