---
from: go
status: draft
concept: "Packages, modules and distribution"
from_way: "A package per directory, capitalised names exported, import cycles rejected by the compiler, a module path in go.mod, and go install module/cmd@version to install binaries from source."
native_way: "A package per directory with __init__.py, underscore names private by convention, import cycles possible but fragile, wheels published to an index, and uv tool install for commands."
sensei_tip: "Go forbids import cycles at compile time; Python allows them and fails at run time with a half-initialised module. Keep Go's discipline: dependencies between your packages should form a tree."
---

| Go | Python |
| --- | --- |
| a directory is a package | a directory with `__init__.py` is a package |
| `Render` (exported) / `render` (unexported) | `render` / `_render` (convention) |
| `import "example.com/invoicer/pdf"` | `from invoicer import pdf` |
| cycles are compile errors | cycles fail at import time: avoid them |
| `cmd/invoicer/main.go` | `[project.scripts]` and `__main__.py` |
| `go install example.com/invoicer@v1.4.0` | `uv tool install invoicer==1.4.0` |
| tags on a VCS repository | wheels uploaded to PyPI or a private index |
