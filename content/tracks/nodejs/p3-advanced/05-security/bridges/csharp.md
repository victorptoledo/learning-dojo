---
from: csharp
status: draft
concept: "Defence in depth"
from_way: "Model validation, EF Core parameterised queries, the Data Protection API, CryptographicOperations.FixedTimeEquals, and NuGet vulnerability audits."
native_way: "Schema validation, parameterised queries, environment or vault secrets, crypto.timingSafeEqual, and npm audit with a lockfile."
sensei_tip: "dotnet restore now warns about vulnerable packages; npm install prints an audit summary too. Make both fail the build in CI instead of scrolling past them."
---

| C# | Node.js |
| --- | --- |
| `[Required]`, `[MaxLength]` | Schema with limits |
| EF Core LINQ / parameters | Parameterised queries |
| `CryptographicOperations.FixedTimeEquals` | `crypto.timingSafeEqual` |
| `RandomNumberGenerator.GetBytes` | `crypto.randomBytes` |
| `NuGetAudit` | `npm audit` |
| `Path.GetFullPath` + prefix check | `path.resolve` + prefix check |
