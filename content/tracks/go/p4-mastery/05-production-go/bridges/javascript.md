---
from: javascript
status: draft
concept: "Shipping Node services"
from_way: "node_modules copied into a node image, npm ci with a lock file, dotenv for configuration, npm audit for vulnerabilities, PM2 or the cluster module to use several cores, and bundlers to shrink the code."
native_way: "One compiled binary in a tiny image, go.sum for verified dependencies, environment variables parsed at start-up, govulncheck, and one process that already uses every core."
sensei_tip: "npm audit lists every advisory in the tree; govulncheck lists the ones you can reach. Expect far fewer, more actionable reports."
---

| Node | Go |
| --- | --- |
| `node:22` image + `node_modules` | distroless image + binary |
| `npm ci` | `go mod download` (verified by go.sum) |
| `dotenv` | environment variables, flags for local runs |
| `npm audit` | `govulncheck` |
| PM2 / cluster | not needed: GOMAXPROCS cores |
| esbuild bundle | `go build -trimpath -ldflags="-s -w"` |
| `process.on("SIGTERM", ...)` | `signal.NotifyContext` + `srv.Shutdown` |
