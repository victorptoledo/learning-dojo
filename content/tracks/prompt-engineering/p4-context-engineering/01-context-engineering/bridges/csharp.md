---
from: csharp
status: draft
concept: "Memory pressure, but for tokens"
from_way: "IMemoryCache with size limits and eviction callbacks, paging with Skip/Take, and background services with their own scope."
native_way: "The context window is memory under pressure on every call: evict old tool results, compact older turns into a summary, page data in by reference, and give sub-agents their own scope."
sensei_tip: "A record type for turns and a `with` expression (turn with { Content = placeholder }) clears a tool result while keeping its call id, so the call and its result stay paired."
---

| C# | Context-engineering concept |
| --- | --- |
| `IMemoryCache` size limit and eviction | clearing old tool results |
| a `with` expression on a record | a placeholder that keeps the call id |
| `Skip/Take` paging | just-in-time retrieval |
| a scoped service per request | a sub-agent with its own context |
| a settings file read at start-up | notes read back in a new session |
