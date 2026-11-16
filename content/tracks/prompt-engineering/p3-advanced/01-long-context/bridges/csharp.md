---
from: csharp
status: draft
concept: "Selecting what to load"
from_way: "Entity Framework queries that load whole object graphs, versus projections, Take(n) and output caching."
native_way: "Select and order the documents a request needs, fit them into a token budget with a marked cut, and keep a stable prefix that the provider can cache."
sensei_tip: "OrderByDescending(d => d.Score).ThenBy(d => d.Id) gives the deterministic order caching needs; a random tie order turns every call into a cache miss."
---

| C# | Long-context concept |
| --- | --- |
| `OrderByDescending(...).ThenBy(...)` | relevance order with a stable tie-break |
| `Take(n)` | a document budget |
| `SecurityElement.Escape` / `WebUtility.HtmlEncode` | escaping document text and metadata |
| output caching | prompt caching |
| a search index query | retrieval instead of stuffing |
