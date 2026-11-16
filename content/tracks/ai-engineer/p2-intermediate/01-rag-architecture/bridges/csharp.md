---
from: csharp
status: draft
concept: "Retrieve, then generate"
from_way: "Azure AI Search or SQL full-text search returning documents to a page."
native_way: "Retrieved passages become the context of an IChatClient call that answers with citations; ingestion keeps the index current."
sensei_tip: "Azure AI Search offers hybrid search with semantic reranking built in; the .NET VectorData abstractions let you test the pipeline against an in-memory store."
---

| C# | RAG step |
| --- | --- |
| Azure AI Search indexer | ingestion |
| hybrid query with semantic ranker | retrieval plus reranking |
| security trimming filters | permission filters |
| Razor or string templates | prompt assembly |
| `Regex.Matches(answer, @"\[(\d+)\]")` | checking citations |
