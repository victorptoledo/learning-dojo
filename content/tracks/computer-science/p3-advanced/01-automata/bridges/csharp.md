---
from: csharp
status: draft
concept: "Regular expressions in .NET"
from_way: "Constructing Regex objects without a timeout."
native_way: ".NET's Regex backtracks by default; you can set a matchTimeout; RegexOptions.NonBacktracking (.NET 7+) uses an automaton with linear time; source generators compile patterns ahead of time."
sensei_tip: "Use `RegexOptions.NonBacktracking` for untrusted input, or always pass a timeout; `[GeneratedRegex]` gives fast, inspectable code."
---

| In C# | Underneath |
| --- | --- |
| `new Regex(p, opts, TimeSpan.FromMilliseconds(100))` | a match timeout |
| `RegexOptions.NonBacktracking` | a DFA-based engine |
| `[GeneratedRegex("...")]` | a source-generated matcher |
| `(?>...)` | atomic group |
| `RegexMatchTimeoutException` | the timeout fired |
