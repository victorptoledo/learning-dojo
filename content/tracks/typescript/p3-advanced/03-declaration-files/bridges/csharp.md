---
from: csharp
status: draft
concept: "Reference assemblies and extension points"
from_way: "Types live in assembly metadata; extension methods and partial classes add members you own; you cannot add a property to someone else's class."
native_way: "Types live in .d.ts files; declaration merging and module augmentation add properties to interfaces you do not own, purely at the type level."
sensei_tip: "Augmentation feels like a partial class for someone else's type, but it only adds types: you still have to set the property at runtime (for example in a middleware)."
---

| C# | TypeScript |
| --- | --- |
| Reference assembly metadata | `.d.ts` files |
| NuGet package | npm package (+ `@types/*` when needed) |
| `partial class` | Interface declaration merging |
| `HttpContext.Items["tenant"]` | Augmented `Request` with a typed `tenant` |
| `GenerateDocumentationFile` | `declaration` + `declarationMap` |
