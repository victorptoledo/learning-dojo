---
from: java
status: draft
concept: "Trusting third-party components"
from_way: "OWASP Dependency-Check or Snyk scans the Maven dependency tree, and releases go to a repository manager such as Nexus with immutable versions."
native_way: "Image scanners cover the same JARs plus the OS packages of the base image; the registry plays Nexus's role, with version tags and digests."
sensei_tip: "The CycloneDX Maven plugin and syft produce the same SBOM format, so one tool chain can answer 'which services ship log4j-core 2.14?' for code and images."
---

| Java | Image supply chain |
| --- | --- |
| Dependency-Check, Snyk | Trivy, Grype, Docker Scout |
| Nexus or Artifactory | a container registry |
| release versions never overwritten | immutable tags, digests |
| signed JARs with jarsigner | cosign or notation signatures |
