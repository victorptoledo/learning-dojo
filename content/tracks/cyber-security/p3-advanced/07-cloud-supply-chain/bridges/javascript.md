---
from: javascript
status: draft
concept: "Trust what you deploy and what deploys it"
from_way: "npm install in CI without a lockfile, install scripts running freely, and actions pinned to @main."
native_way: "npm ci from a committed lockfile, npm audit or OSV scanning, provenance-signed packages, and actions pinned by SHA with minimal permissions."
sensei_tip: "`npm publish --provenance` from GitHub Actions links a package to its source and build, and `npm audit signatures` checks registry signatures and provenance of what you install."
---

| JavaScript / TypeScript | Supply chain practice |
| --- | --- |
| npm install in CI | npm ci |
| uses: some/action@main | uses: some/action@<full sha> |
| default GITHUB_TOKEN permissions | permissions: contents: read |
| unscoped internal packages | a scope tied to your registry |
| no SBOM | npm sbom or CycloneDX |
