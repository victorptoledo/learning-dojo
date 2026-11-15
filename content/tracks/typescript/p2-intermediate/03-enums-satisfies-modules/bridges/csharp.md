---
from: csharp
status: draft
concept: "Enums and namespaces"
from_way: "enum types (numeric underneath, with [Flags]), namespaces and assemblies; switch expressions on enums."
native_way: "String literal unions and as const objects instead of enums; ES modules instead of namespaces; satisfies for checked constant tables."
sensei_tip: "TypeScript's enum looks like C#'s, which is exactly why C# developers reach for it. In modern TypeScript, prefer the union: it is erasable and works with plain JSON."
---

| C# | TypeScript |
| --- | --- |
| `enum Level { Debug, Info }` | `type Level = "debug" \| "info"` |
| `[Flags] enum Perm` | A union array, or a bit field of number constants |
| `namespace Acme.Money` | A module file `money.ts` |
| `using Acme.Money;` | `import { ... } from "./money.ts"` |
| `internal` | Not exported, or not listed in `package.json` `exports` |
