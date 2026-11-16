---
from: go
status: draft
concept: "Build pipelines"
from_way: "go build: one static binary, dead-code elimination by the linker, embed for assets."
native_way: "A frontend build is the same idea for the browser: one graph from an entry point, dead code removed, assets embedded or hashed."
sensei_tip: "The Go linker drops unused functions; tree shaking does that for ES modules. Both need code without import-time side effects."
---

| Go | In the browser |
| --- | --- |
| `go build` | `vite build` |
| Linker dead-code elimination | Tree shaking |
| `//go:embed static/*` | Assets copied with hashed names |
| `-ldflags "-X main.version=..."` | Build-time constants in `import.meta.env` |
