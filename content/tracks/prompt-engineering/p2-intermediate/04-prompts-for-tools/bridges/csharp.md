---
from: csharp
status: draft
concept: "Actions with model binding"
from_way: "ASP.NET controller actions with model binding, data annotations and [Authorize] attributes."
native_way: "A tool is an action the model invokes: a name, a description and a schema; your dispatcher validates the arguments and authorises the call before it runs."
sensei_tip: "ValidationProblemDetails is the right shape for a tool error result: a list of field errors in plain words that the model can read and fix."
---

| C# | Tool-use concept |
| --- | --- |
| a controller action | a tool |
| `[Required]`, `[Range]` annotations | the parameter schema |
| `ValidationProblemDetails` | an error result |
| `[Authorize(Policy = ...)]` | permission checks in code |
| `Dictionary<string, Func<...>>` | a dispatcher |
