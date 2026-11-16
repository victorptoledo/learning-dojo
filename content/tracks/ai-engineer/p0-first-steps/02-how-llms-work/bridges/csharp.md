---
from: csharp
status: draft
concept: "Tokens and next-token prediction"
from_way: "string.Length counts chars, and an async method returns one deterministic Task result."
native_way: "Text is split into tokens, and the model returns them one at a time from a sampled distribution, which maps to IAsyncEnumerable streaming."
sensei_tip: "Microsoft.ML.Tokenizers can count tokens for several model families locally; use it, or the provider's API, to enforce budgets instead of character counts."
---

| C# idea | LLM idea |
| --- | --- |
| `string.Length` | the token count, per tokenizer |
| `Random.Shared` | sampling with temperature and top-p |
| `IAsyncEnumerable<string>` | a streamed response |
| session state in the server | the history your code resends each call |
| `MaxRequestBodySize` | the context window |
