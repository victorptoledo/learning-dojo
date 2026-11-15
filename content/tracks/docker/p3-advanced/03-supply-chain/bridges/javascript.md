---
from: javascript
status: draft
concept: "Trusting third-party components"
from_way: "npm audit reports vulnerable packages, and npm provenance links a package to the CI run that published it."
native_way: "Scanners report vulnerable node_modules and OS packages in the image; BuildKit provenance and cosign do for images what npm provenance does for packages."
sensei_tip: "A big part of a Node image's CVEs often comes from the full node base; node:24-alpine or a distroless nodejs image removes most of them."
---

| JavaScript | Image supply chain |
| --- | --- |
| `npm audit` | `docker scout cves`, `trivy image` |
| `npm publish --provenance` | `buildx --provenance`, cosign |
| `dist-tags` | moving image tags |
| `package-lock.json` | an SBOM of the image |
