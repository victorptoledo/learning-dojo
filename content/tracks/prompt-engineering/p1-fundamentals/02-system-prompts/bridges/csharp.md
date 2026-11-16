---
from: csharp
status: draft
concept: "Options and per-request input"
from_way: "IOptions<T> configuration bound at startup, and controller actions that receive a model per request."
native_way: "The system prompt is the bound configuration; the user turn is the per-request model; tools enforce the rules that matter."
sensei_tip: "Keep the system prompt in one place, like a strongly typed options class, and build each request's user turn from a record. Mixing the two is how per-user data ends up cached for everyone."
---

| C# / ASP.NET | Prompt concept |
| --- | --- |
| `IOptions<AssistantOptions>` | the system prompt |
| a request DTO | the user turn |
| `[Authorize]` policies | enforcement in code |
| user secrets / Key Vault | never in the prompt |
| `List<ChatMessage>` | the message history |
