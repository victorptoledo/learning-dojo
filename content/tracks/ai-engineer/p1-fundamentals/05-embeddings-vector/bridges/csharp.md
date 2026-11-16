---
from: csharp
status: draft
concept: "Search by meaning instead of by keyword"
from_way: "SQL Server full-text search, Azure AI Search keyword queries or LINQ Contains."
native_way: "Embeddings stored with Microsoft.Extensions.VectorData connectors (Azure AI Search, Qdrant, PostgreSQL) and searched by similarity, often in hybrid mode."
sensei_tip: "System.Numerics.Tensors' TensorPrimitives.CosineSimilarity computes similarity with SIMD on float spans, handy for in-memory search and tests."
---

| C# | Vector search |
| --- | --- |
| `CONTAINS()` full-text | keyword search |
| `ReadOnlyMemory<float>` | an embedding |
| `TensorPrimitives.CosineSimilarity` | similarity |
| Azure AI Search hybrid query | BM25 plus vectors fused |
| `IEmbeddingGenerator` | an abstraction over embedding models |
