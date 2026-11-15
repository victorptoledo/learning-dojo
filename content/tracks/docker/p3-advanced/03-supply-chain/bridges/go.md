---
from: go
status: draft
concept: "Trusting third-party components"
from_way: "govulncheck reports vulnerabilities that your code actually calls, and the checksum database verifies modules."
native_way: "Image scanners read the module versions embedded in Go binaries (go version -m) and the base image packages; digests and signatures verify the image."
sensei_tip: "A Go binary on distroless/static has almost nothing for a scanner to report except your modules and the Go standard library version, so keep the toolchain current."
---

| Go | Image supply chain |
| --- | --- |
| `govulncheck` | `trivy image` reads the binary's build info |
| `sum.golang.org` | digests and signatures |
| `go version -m server` | an SBOM |
| tagging a module `v1.4.0` | pushing `1.4.0` and `sha-<commit>` tags |
