---
from: python
status: draft
concept: "Trusting third-party components"
from_way: "pip-audit checks installed packages against advisories, and hashes in a lock file make installs reproducible."
native_way: "Image scanners read both site-packages and the OS packages, and digests do for whole images what pip hashes do for packages."
sensei_tip: "Trivy finds vulnerable Python packages inside the image even when no requirements file was copied, because it reads the installed package metadata."
---

| Python | Image supply chain |
| --- | --- |
| `pip-audit` | `trivy image`, `grype` |
| `--require-hashes` | deploying by `@sha256:` digest |
| PyPI trusted publishing | keyless signing from CI with cosign |
| `pip list --format=json` | an SBOM (SPDX, CycloneDX) |
