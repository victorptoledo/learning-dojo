---
from: go
status: draft
concept: "iota constants and packages"
from_way: "Named constants with iota for enums, packages for modules, exported names start with a capital letter, go.mod for the module path."
native_way: "Union types and as const objects for enumerations, files as modules with explicit export, package.json exports and imports for package boundaries."
sensei_tip: "The Go idiom `type Level string; const Debug Level = \"debug\"` is the as const object pattern. Unlike Go, TypeScript can check that a switch handles every level."
---

| Go | TypeScript |
| --- | --- |
| `type Level int; const (Debug Level = iota; Info)` | `const Level = { Debug: "debug", Info: "info" } as const` |
| Capitalised = exported | `export` |
| `internal/` packages | `package.json` `imports` (`#internal/...`) and unlisted paths |
| `import "acme.com/money"` | `import { ... } from "@acme/money"` |
