---
from: java
status: draft
concept: "Security checks run in the delivery path"
from_way: "The OWASP Dependency-Check Maven plugin runs before a release, and a pentest happens once a year."
native_way: "Dependency scanning, CodeQL, image scanning and secret scanning run on every pull request; images are signed and the cluster verifies them."
sensei_tip: "Generate a CycloneDX SBOM with the cyclonedx-maven-plugin; it lists every transitive JAR, which is what Log4Shell made everyone wish they had."
---

| Java world | DevSecOps |
| --- | --- |
| OWASP Dependency-Check | SCA in every PR |
| SpotBugs Find Security Bugs | SAST |
| a signed JAR | a signed image with provenance |
| a keystore file | a secrets manager |
