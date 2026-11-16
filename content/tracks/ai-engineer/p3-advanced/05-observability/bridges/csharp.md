---
from: csharp
status: draft
concept: "See what the model saw"
from_way: "ILogger, Application Insights and OpenTelemetry for ASP.NET Core."
native_way: "Chat-client middleware that emits OpenTelemetry spans and metrics with gen_ai attributes, and logs prompts with redaction."
sensei_tip: "Microsoft.Extensions.AI's UseOpenTelemetry() on the chat client pipeline emits GenAI-convention spans and token metrics; enable sensitive-data capture only where policy allows."
---

| C# | LLM observability |
| --- | --- |
| `ILogger` scopes | conversation id in logs |
| `ActivitySource` | spans |
| `UseOpenTelemetry()` on IChatClient | gen_ai spans and metrics |
| Application Insights | dashboards and alerts |
| `EnableSensitiveData = false` | keeping prompts out of telemetry |
