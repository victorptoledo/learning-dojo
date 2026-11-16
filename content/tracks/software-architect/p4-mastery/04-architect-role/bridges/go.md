---
from: go
status: draft
concept: "The architect's role and governance"
from_way: "A list of approved Go modules on a wiki page and a reviewer who checks every `go.mod` change."
native_way: "A service template repository as the paved road, a shared `golangci-lint` configuration with `depguard` rules, ADRs and an advice forum, and internal libraries for the cross-cutting concerns."
sensei_tip: "`golangci-lint` with a shared configuration turns many architectural rules (allowed imports, forbidden packages) into a check every developer runs before pushing."
---

| Go world | Governance idea |
| --- | --- |
| a template repository, `gonew` | paved road |
| shared `golangci-lint` config with `depguard` | fitness functions as governance |
| internal libraries for logging, auth, tracing | paved road for cross-cutting concerns |
| `docs/adr` and an advisory forum | the advice process |
| Renovate or Dependabot | convergence on supported versions |
| spikes and prototypes | staying technical |
