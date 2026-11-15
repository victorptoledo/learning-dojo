---
from: csharp
status: draft
concept: "Code smells and refactoring"
from_way: "Partial classes hiding god classes, regions labelling sections of long methods, magic strings everywhere."
native_way: "Characterisation tests with Verify (snapshot testing), Rider or Visual Studio refactorings, records and enums for primitive obsession, and Roslyn analyzers to find candidates."
sensei_tip: "`#region` blocks inside a method are a comment smell with folding: each region is usually an Extract Method waiting to happen."
---

| C# | Refactoring tool or idea |
| --- | --- |
| Rider / Visual Studio refactorings | automated moves |
| Verify snapshot tests | characterisation tests |
| `record`, `enum` | fix primitive obsession |
| Roslyn analyzers, SonarLint | smell detection |
| NetArchTest | keeping structure |
