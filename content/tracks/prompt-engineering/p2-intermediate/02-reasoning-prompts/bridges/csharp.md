---
from: csharp
status: draft
concept: "Working and result"
from_way: "Complex LINQ expressions in one line, or breaking them into named steps you can inspect."
native_way: "The model writes named steps in <thinking>, then the result in <answer>; code extracts the answer and can vote over several samples."
sensei_tip: "answers.GroupBy(a => a).OrderByDescending(g => g.Count()).First() is self-consistency; normalise the strings first so formatting differences do not split the vote."
---

| C# | Reasoning concept |
| --- | --- |
| intermediate variables in a method | chain-of-thought steps |
| the return value | the `<answer>` tag |
| `Regex.Matches(text, "<answer>(.*?)</answer>", RegexOptions.Singleline)` | extracting the answer |
| `GroupBy` plus `Count` | self-consistency vote |
| `ILogger.LogDebug` | reasoning kept out of user output |
