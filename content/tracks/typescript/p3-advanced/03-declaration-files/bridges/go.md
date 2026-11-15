---
from: go
status: draft
concept: "Types travel with the source"
from_way: "Packages are compiled from source, so their types are always known exactly; there is nothing like a separate type description file."
native_way: "JavaScript packages may ship no types; .d.ts files and @types packages describe them, and may be incomplete or out of date."
sensei_tip: "In Go the compiler knows every dependency's types. In TypeScript, a dependency's types are a claim made by its .d.ts: when behaviour and types disagree, the runtime wins."
---

| Go | TypeScript |
| --- | --- |
| Types from the package source | `.d.ts` shipped or from `@types/*` |
| `go doc` | Hover and go to definition in the `.d.ts` |
| No way to extend another package's struct | Module augmentation of interfaces |
| cgo headers for C code | `.d.ts` for JavaScript code |
