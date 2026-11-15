---
from: javascript
status: draft
concept: "Packages, dependencies and builds"
from_way: "package.json and a lock file, node_modules per project, npm scripts for build, lint and test, Prettier and ESLint configured by hand, and a Node runtime where the code runs."
native_way: "go.mod and go.sum, a shared module cache instead of node_modules, the go command for build, test, vet and format, and a native binary as the output."
sensei_tip: "Go has no equivalent of npm scripts by design. If you need one, a short Makefile or a tool directive in go.mod is enough."
---

| JavaScript / Node | Go |
| --- | --- |
| `npm init` | `go mod init example.com/app` |
| `package.json` + `package-lock.json` | `go.mod` + `go.sum` |
| `node_modules/` | the module cache, outside the project |
| `npm install lodash` | `go get module@version` |
| `export` / `import` | Upper case names are exported; `import "path"` |
| Prettier | `gofmt` |
| ESLint | `go vet` (plus staticcheck or golangci-lint) |
| `node app.js` | `go run .` or a compiled binary |
