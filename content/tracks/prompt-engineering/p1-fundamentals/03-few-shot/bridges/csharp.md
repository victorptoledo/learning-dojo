---
from: csharp
status: draft
concept: "Theory data for a model"
from_way: "xUnit [Theory] tests with [InlineData] rows covering normal and edge cases."
native_way: "Few-shot examples are inline data for the model: a balanced set of inputs and exact outputs that covers the edges, formatted the same way each time."
sensei_tip: "Store examples as records and render them with one method; a LINQ GroupBy on the label tells you in one line whether your set is balanced."
---

| C# | Few-shot concept |
| --- | --- |
| `[InlineData(...)]` rows | examples |
| `record Example(string Input, string Output)` | one labelled example |
| `examples.GroupBy(e => e.Output)` | checking label balance |
| `[MemberData]` loaded from a file | an example pool kept in a data file |
| one `ToString()` format | one consistent example format |
