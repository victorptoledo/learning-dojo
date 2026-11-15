---
from: csharp
status: draft
concept: "A compiler that erases its types"
from_way: "dotnet build compiles C# to IL; types are enforced and kept at runtime (reflection, generics are reified)."
native_way: "tsc only checks; the output is JavaScript without types, and generics do not exist at runtime."
sensei_tip: "TypeScript and C# share a designer (Anders Hejlsberg), so the syntax feels familiar, but the runtime model is JavaScript's: no reflection on types, no casts that throw."
---

| C# | TypeScript |
| --- | --- |
| `dotnet build` | `tsc` |
| `dotnet run` | `node app.ts` |
| `.csproj` with `<Nullable>enable</Nullable>` | `tsconfig.json` with `"strict": true` |
| `typeof(T)` at runtime | No runtime types: `typeof x` is JavaScript's operator |
