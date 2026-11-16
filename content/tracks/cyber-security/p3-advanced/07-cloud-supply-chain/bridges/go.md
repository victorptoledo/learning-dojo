---
from: go
status: draft
concept: "Trust what you deploy and what deploys it"
from_way: "go get of latest versions and a GOPROXY=direct setting to fetch straight from repositories."
native_way: "go.mod and go.sum checked by the checksum database, the module proxy, govulncheck in CI, and GOPRIVATE for internal modules."
sensei_tip: "`govulncheck ./...` reports only vulnerabilities in functions your code actually calls, which makes its findings far more actionable than a plain version match."
---

| Go | Supply chain practice |
| --- | --- |
| GOFLAGS=-mod=mod in CI | -mod=readonly with go.sum |
| GONOSUMDB for everything | GOPRIVATE only for internal modules |
| no vulnerability scan | govulncheck ./... |
| FROM golang:latest | pinned digests |
| static cloud keys | OIDC to a scoped role |
