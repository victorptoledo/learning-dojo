---
from: csharp
status: draft
concept: "Contracts and XML documentation"
from_way: "XML doc comments with <summary>, <param>, <returns> and <exception>, and nullable reference types that make the contract explicit."
native_way: "A prompt that documents its own contract: what it does, what it needs, what it returns and what it returns when it cannot do the job."
sensei_tip: "The <exception> tag of a C# method is the model's way out: name the condition and the exact value to return, so callers can handle it like any other result."
---

| C# | Prompt concept |
| --- | --- |
| `<summary>` | the task in one sentence |
| `<param>` | the context and input the prompt needs |
| `<returns>` | the output format |
| `<exception>` | the explicit exit (`NOT_AN_ORDER`) |
| unit tests with xUnit | success criteria |
