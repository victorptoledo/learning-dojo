---
from: csharp
status: draft
concept: "Client-server communication"
from_way: "ASP.NET Core results such as Ok(), Created() and NotFound(), and HttpClient."
native_way: "The HTTP contract behind those helpers."
sensei_tip: "Results.Created and CreatedAtAction set the Location header for you; ResponseCaching and OutputCache middleware implement Cache-Control behaviour you should understand first."
---

| C# | In practice |
| --- | --- |
| Ok(dto) | 200 |
| CreatedAtAction(...) | 201 with Location |
| NoContent() | 204 |
| [ResponseCache] | Cache-Control |
| HttpClient | a client that respects these rules |
