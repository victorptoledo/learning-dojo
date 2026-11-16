---
from: csharp
status: draft
concept: "An LLM feature is a service with one probabilistic dependency"
from_way: "ASP.NET Core services with injected dependencies, typed HttpClient calls and xUnit asserts on exact results."
native_way: "The same dependency-injected service, with a model client whose output is validated, evaluated over example sets and checked before it reaches users or tools."
sensei_tip: "Register the model client behind an interface (Microsoft.Extensions.AI's IChatClient is one) so tests use a fake and production adds retries, logging and telemetry as middleware."
---

| C# habit | In an LLM feature |
| --- | --- |
| `Assert.Equal(expected, actual)` | a pass rate over a golden set |
| typed `HttpClient` DTOs | structured output validated against a JSON schema |
| Polly policies | retries, timeouts and fallbacks around the model call |
| business rules in services | still in code; the model handles fuzzy language |
| Semantic Kernel, Microsoft.Extensions.AI | frameworks over the same parts: model, context, tools, evals |
