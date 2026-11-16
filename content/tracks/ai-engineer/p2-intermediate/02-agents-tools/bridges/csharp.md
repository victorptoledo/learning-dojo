---
from: csharp
status: draft
concept: "The model proposes calls, your code executes them"
from_way: "MediatR handlers or a switch over command types, with model validation."
native_way: "AIFunction tools created from methods (AIFunctionFactory.Create) or Semantic Kernel plugins; a function-invoking chat client runs the loop."
sensei_tip: "Microsoft.Extensions.AI's UseFunctionInvocation middleware runs the tool loop; set MaximumIterationsPerRequest and keep authorisation inside the functions."
---

| C# | Tool calling |
| --- | --- |
| a MediatR handler per command | one tool per action |
| `[Description]` attributes | tool and parameter descriptions |
| `AIFunctionFactory.Create(method)` | a tool definition |
| `ProblemDetails` | an error tool result |
| `FunctionInvokingChatClient` | the agent loop with limits |
