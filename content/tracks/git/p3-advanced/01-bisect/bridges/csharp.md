---
from: csharp
status: draft
concept: "Automated regression hunting"
from_way: "Comparing two builds' behaviour in the debugger and diffing whole assemblies."
native_way: "git bisect run with dotnet build and dotnet test --filter on one test."
sensei_tip: "dotnet test --filter FullyQualifiedName~CheckoutTotals keeps every step to a few seconds. Map a failed restore or build to exit 125."
---

| .NET habit | With bisect |
| --- | --- |
| "broke between 2.3 and 2.4" | `git bisect start v2.4.0 v2.3.0` |
| `dotnet build` fails at a commit | `exit 125` |
| one xUnit or NUnit test | `dotnet test --filter ...` as the run command |
| the culprit found | `git show <hash>`, then a regression test |
