---
from: csharp
status: draft
concept: "Theories, traits and gates"
from_way: "xUnit theories with data sets, traits to group tests, and a build pipeline that blocks on failures."
native_way: "A tagged evaluation set, graders per criterion, several runs per case to handle variance, and a ship rule that blocks regressions."
sensei_tip: "Use the same habit as [Trait(\"Category\", ...)]: every case gets tags, and the report groups by them. Most real prompt regressions are visible in one slice only."
---

| C# | Evaluation concept |
| --- | --- |
| `[Theory]` with `[MemberData]` | the test set |
| `[Trait("Category", "pt-BR")]` | slices |
| `Assert.Equal` | an exact-match grader |
| FluentAssertions `.Match(...)` | a pattern grader |
| a failing pipeline stage | the ship rule |
