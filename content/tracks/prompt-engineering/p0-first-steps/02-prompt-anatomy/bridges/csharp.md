---
from: csharp
status: draft
concept: "Composite formatting and named holes"
from_way: "Interpolated strings and string.Format with numbered holes, and raw string literals for multi-line text."
native_way: "A prompt template with named variables and delimited sections, rendered by a function that checks every hole is filled."
sensei_tip: "C# raw string literals (\"\"\"...\"\"\") are ideal for prompt templates: no escaping, real line breaks. Keep them in one class per prompt, with the variables as properties of a record."
---

| C# | Prompt concept |
| --- | --- |
| `$"Summarise {email}"` | an inline prompt |
| `"""raw string literal"""` | a multi-line template |
| a `record EmailPromptInput(...)` | the template's typed variables |
| `string.Format("{0}", x)` | positional holes (prefer names) |
| `ArgumentNullException` | fail on a missing variable |
