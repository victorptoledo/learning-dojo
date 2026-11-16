---
from: csharp
status: draft
concept: "A model call is a remote call with a richer result"
from_way: "HttpClient with EnsureSuccessStatusCode, Polly for retries, and typed DTOs."
native_way: "An SDK client (Anthropic, OpenAI or Azure OpenAI for .NET, or Microsoft.Extensions.AI's IChatClient) whose result has content, a finish reason and usage."
sensei_tip: "IChatClient's GetStreamingResponseAsync returns IAsyncEnumerable updates; await foreach them into the UI, and check FinishReason on the final update before trusting the text."
---

| C# | Model API |
| --- | --- |
| `EnsureSuccessStatusCode()` | status code plus `FinishReason` |
| Polly retry on transient errors | retry 429, 5xx, overloaded |
| `IAsyncEnumerable<T>` | a streamed response |
| `IConfiguration["Key"]`, user secrets | the key outside the code |
| a mock `HttpMessageHandler` | a fake `IChatClient` |
