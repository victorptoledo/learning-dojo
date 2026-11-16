---
from: csharp
status: draft
concept: "Calling a probabilistic service"
from_way: "Strongly typed method calls, deterministic results for the same input, and exceptions for invalid arguments."
native_way: "One string (or a list of messages) goes in, a sampled string comes out, and nothing enforces that it is correct; size and cost are counted in tokens."
sensei_tip: "Treat a model like an HttpClient call to a service you do not control: set limits (max tokens, timeouts), log what you sent and received, and validate the response before you use it."
---

| C# habit | With a language model |
| --- | --- |
| `string.Length` | token count |
| a pure method | a sampled answer that can vary per run |
| `ArgumentException` on bad input | the model always answers, even when it should not |
| `Random` with a seed | temperature and sampling settings |
| session state in ASP.NET | the conversation resent with each request |
