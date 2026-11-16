---
from: csharp
status: draft
concept: "Untrusted text that gives orders"
from_way: "Parameterised SqlCommand, Razor's automatic encoding and policy-based authorisation."
native_way: "The same defences around the model, plus least-privilege functions, approval steps and content filters before and after the call."
sensei_tip: "Azure AI Content Safety has a Prompt Shields feature that flags direct and indirect injection attempts; use it as a detection layer, not as the only control."
---

| C# | LLM guardrail |
| --- | --- |
| `SqlParameter` | model output as parameters |
| Razor encoding | escaping output |
| `[Authorize(Policy = ...)]` | authorisation in each function |
| Prompt Shields, content filters | detection of injection and unsafe content |
| `ILogger` with redaction | masking PII in logs |
