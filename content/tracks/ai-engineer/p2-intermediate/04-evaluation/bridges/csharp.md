---
from: csharp
status: draft
concept: "Tests become measurements"
from_way: "xUnit or NUnit tests with [Theory] data, run in CI."
native_way: "An evaluation run that scores each case with metrics (Microsoft.Extensions.AI.Evaluation has quality evaluators), reports per slice and compares runs."
sensei_tip: "Microsoft.Extensions.AI.Evaluation includes LLM-based evaluators and a report tool; treat its scores like any metric: calibrate them on your data before you gate on them."
---

| .NET testing | LLM evaluation |
| --- | --- |
| `[Theory]` with `[MemberData]` | golden cases |
| `Assert.Equal` | exact metric |
| an evaluator | an LLM judge with a rubric |
| test categories | slices |
| a CI quality gate | a score gate |
