---
from: csharp
status: draft
concept: "Defence in depth"
from_way: "Parameterised queries, HtmlEncoder for output, anti-forgery tokens and authorisation policies."
native_way: "No single control stops prompt injection: escape and label untrusted text, flag suspicious content, restrict tools, check outputs and confirm risky actions."
sensei_tip: "Use HtmlEncoder-style escaping for text going into the prompt and Uri.TryCreate plus a host allow-list for links coming out; the two checks guard the two directions."
---

| C# | Prompt-injection concept |
| --- | --- |
| `SqlParameter` | no exact equivalent: escape and delimit instead |
| `HtmlEncoder.Default.Encode` | escaping untrusted text |
| `[Authorize]` on each action | permissions in the tools' code |
| `Uri.TryCreate` and a host allow-list | checking output links |
| a two-step confirmation | human confirmation before acting |
