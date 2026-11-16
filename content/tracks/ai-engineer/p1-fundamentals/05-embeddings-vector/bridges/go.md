---
from: go
status: draft
concept: "Search by meaning instead of by keyword"
from_way: "Bleve or Elasticsearch for full-text, or strings.Contains over a slice."
native_way: "[]float32 embeddings from an API, stored in pgvector or a vector database (Weaviate and Milvus are written in Go), searched by similarity."
sensei_tip: "A []float32 dot product in a loop is fast enough for tests and small corpora; keep vectors normalised at write time so search is just multiply-and-add."
---

| Go | Vector search |
| --- | --- |
| `strings.Contains` | keyword matching |
| `[]float32` | an embedding |
| a loop summing `a[i]*b[i]` | dot product |
| Bleve | the keyword side of hybrid search |
| pgvector-go, Qdrant client | ANN search with filters |
