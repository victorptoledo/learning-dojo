---
from: csharp
status: reviewed
concept: "EF Core migrations without downtime"
from_way: "A generated migration that renames a property and runs at application start."
native_way: "Additive migrations reviewed for locking, data backfills outside the deploy, Microsoft.FeatureManagement for flags, and deployment slots or canaries for gradual release."
sensei_tip: "Read the SQL of every EF Core migration (dotnet ef migrations script) before it ships: a rename may be generated as a drop and an add."
---

| .NET | Production ownership |
| --- | --- |
| `RenameColumn` in a migration | expand and contract |
| `dotnet ef migrations script` | review the real SQL |
| `Microsoft.FeatureManagement` | feature flags |
| deployment slots | swap and swap back |
