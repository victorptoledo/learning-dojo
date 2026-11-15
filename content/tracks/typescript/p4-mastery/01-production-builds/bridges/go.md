---
from: go
status: draft
concept: "A single binary vs a JavaScript bundle"
from_way: "go build produces one static binary with types checked; deployment copies that file."
native_way: "Deploy the sources with node_modules, compiled dist/ output, or a bundle; type-checking is a separate tsc step (and tsc itself is now a Go binary)."
sensei_tip: "A bundle with esbuild is the closest thing to a Go binary: one file, fast start-up. Keep tsc in CI, because the bundler does not check anything."
---

| Go | TypeScript |
| --- | --- |
| `go build -o app` | `esbuild src/main.ts --bundle --platform=node --outfile=app.js` |
| `go vet ./...` in CI | `tsc --noEmit` + `eslint .` |
| `go.sum` | `package-lock.json` |
| `-trimpath`, reproducible builds | `npm ci` with a lock file |
| `GOMAXPROCS` | `tsc --checkers N` |
