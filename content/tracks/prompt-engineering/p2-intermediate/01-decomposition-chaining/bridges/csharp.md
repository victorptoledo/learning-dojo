---
from: csharp
status: draft
concept: "Pipelines and policies"
from_way: "LINQ pipelines, MediatR pipeline behaviours, and Polly retry policies around HTTP calls."
native_way: "A chain of prompts with typed hand-offs, a validation gate after each step, bounded retries and a budget."
sensei_tip: "Polly's retry policy with a maximum count is exactly the bounded retry a step needs; put the validation inside the policy so a bad answer counts as a failure."
---

| C# | Chaining concept |
| --- | --- |
| a LINQ pipeline | a sequential chain |
| FluentValidation after each step | a gate |
| a strategy chosen by a factory | routing |
| `Task.WhenAll(...)` | parallel sections |
| Polly `RetryAsync(2)` | bounded retries |
