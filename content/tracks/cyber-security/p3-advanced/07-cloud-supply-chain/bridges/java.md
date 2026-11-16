---
from: java
status: draft
concept: "Trust what you deploy and what deploys it"
from_way: "Maven builds that resolve version ranges and plugins from anywhere, with cloud keys in ~/.aws/credentials on the build server."
native_way: "Pinned versions, a repository manager as the only source, dependency and plugin verification, CycloneDX SBOMs, signed artifacts, and OIDC-based CI credentials."
sensei_tip: "The `cyclonedx-maven-plugin` produces an SBOM in one build step, and Maven's checksum policy plus a repository manager (Nexus, Artifactory) stop dependency confusion."
---

| Java | Supply chain practice |
| --- | --- |
| version ranges [1.0,) | exact versions, dependency management |
| Maven Central and random repos | one repository manager |
| no SBOM | cyclonedx-maven-plugin |
| access keys on the CI server | OIDC to a scoped role |
| OWASP check once a year | dependency scanning on every build |
