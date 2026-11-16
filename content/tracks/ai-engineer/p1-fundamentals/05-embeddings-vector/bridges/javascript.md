---
from: javascript
status: draft
concept: "Search by meaning instead of by keyword"
from_way: "Algolia, Elasticsearch or a filter with includes() over an array."
native_way: "Embeddings from an API, stored in a vector database or pgvector, and searched by cosine similarity, with keyword search fused in."
sensei_tip: "Float32Array is the natural container for embeddings in JavaScript; compute a dot product in a plain loop for small sets, and leave large sets to the database."
---

| JavaScript | Vector search |
| --- | --- |
| `arr.filter(s => s.includes(q))` | keyword matching, misses synonyms |
| `Float32Array` | an embedding |
| a reduce over two arrays | dot product |
| Algolia, Elasticsearch | the keyword side of hybrid search |
| a vector DB client (Qdrant, Pinecone) | ANN search with metadata filters |
