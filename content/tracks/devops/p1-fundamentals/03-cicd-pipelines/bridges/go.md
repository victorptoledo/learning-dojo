---
from: go
status: draft
concept: "The pipeline is a build file for the whole delivery"
from_way: "A Makefile with vet, test and build targets run before pushing."
native_way: "The workflow runs the same targets on a fresh runner; setup-go caches the module and build caches keyed by go.sum."
sensei_tip: "Run go test -race in CI even if it is slow locally: data races found there are cheap, in production they are not."
---

| Go world | CI pipeline |
| --- | --- |
| `make test` | a test job |
| `go.sum` | the cache key |
| `GOFLAGS=-mod=readonly` | reproducible builds |
| `golangci-lint run` | a lint job |
