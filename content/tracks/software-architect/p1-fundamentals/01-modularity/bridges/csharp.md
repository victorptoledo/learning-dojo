---
from: csharp
status: draft
concept: "Modularity and package dependencies"
from_way: "Every class `public`, a `Common` project referenced by every other project, and namespaces that do not match assemblies."
native_way: "Assemblies (projects) that hide a decision each, `internal` by default, `InternalsVisibleTo` only for tests, project references that form a graph without cycles, and NetArchTest or ArchUnitNET rules."
sensei_tip: "MSBuild already refuses cycles between projects, but not between namespaces inside one project. Make classes `internal` and check namespace rules with ArchUnitNET."
---

| C# | Modularity idea |
| --- | --- |
| `internal` types | information hiding inside an assembly |
| project references | components with acyclic dependencies |
| `InternalsVisibleTo` | a controlled exception for tests |
| NetArchTest, ArchUnitNET | dependency rules as tests |
| NDepend | Ca, Ce, instability, abstractness, the main sequence |
| a `Common` project used everywhere | the zone of pain |
