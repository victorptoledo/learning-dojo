---
from: javascript
status: draft
concept: "Modularity and package dependencies"
from_way: "Deep relative imports (`../../../shared/utils`) from any file to any file, and a `shared` folder everyone imports."
native_way: "Packages or folders with an `index.ts` public interface, `exports` in `package.json` to hide internals, workspaces in a monorepo, and `dependency-cruiser` or Nx module boundary rules against cycles."
sensei_tip: "ES modules allow cycles and only fail at runtime with an undefined import. Add a `no-circular` rule to `dependency-cruiser` or `eslint-plugin-import` before the first one appears."
---

| JavaScript / TypeScript | Modularity idea |
| --- | --- |
| `exports` field in `package.json` | a package's published interface |
| an `index.ts` per folder | the module's interface; the rest is internal |
| `dependency-cruiser` `no-circular` | the acyclic dependencies principle |
| Nx `enforce-module-boundaries` | allowed dependencies between libraries |
| `madge` | the import graph, for metrics |
| a `shared` folder used everywhere | the zone of pain |
