---
from: javascript
status: draft
concept: "Code smells and refactoring"
from_way: "Callback pyramids, objects with dozens of optional properties, and refactoring without types."
native_way: "Jest snapshot tests as characterisation tests, TypeScript (even with `allowJs`) as the refactoring safety net, small modules, and ESLint complexity rules to find candidates."
sensei_tip: "Migrating a file to TypeScript is itself a refactoring with a free safety net: the compiler lists every place a shape is used inconsistently."
---

| JavaScript | Refactoring tool or idea |
| --- | --- |
| VS Code / WebStorm refactorings | automated moves |
| Jest snapshots | characterisation tests |
| TypeScript | safety net, value types |
| ESLint `complexity`, `max-lines-per-function` | smell detection |
| small ES modules | fix god files |
