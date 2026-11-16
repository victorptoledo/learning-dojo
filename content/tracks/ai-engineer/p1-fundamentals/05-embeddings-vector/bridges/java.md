---
from: java
status: draft
concept: "Search by meaning instead of by keyword"
from_way: "Lucene, Elasticsearch or Hibernate Search with analyzers and BM25 scoring."
native_way: "Vectors from an embedding model stored in a vector index (pgvector, a vector database, or Lucene's HNSW vector fields) and searched by cosine similarity, often fused with BM25."
sensei_tip: "Lucene and Elasticsearch already ship HNSW dense-vector fields, so a team on Elasticsearch can add semantic and hybrid search without a new database."
---

| Java / Lucene | Vector search |
| --- | --- |
| an analyzer and tokens | an embedding model |
| BM25 score | cosine similarity |
| inverted index | HNSW graph or IVF clusters |
| filter query | metadata filter |
| `BooleanQuery` of two queries | hybrid search fused with RRF |
| Spring AI `VectorStore` | an abstraction over pgvector, Qdrant, Elasticsearch |
