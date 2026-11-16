---
from: python
status: draft
concept: "Search by meaning instead of by keyword"
from_way: "Full-text search through a database or a library like Whoosh, or a substring filter in a list comprehension."
native_way: "NumPy arrays of embeddings with a dot product for small sets, and FAISS, pgvector or a vector database client for large ones."
sensei_tip: "For up to about a hundred thousand vectors, `scores = matrix @ query` on normalised NumPy arrays plus `argpartition` for the top k is exact, fast and needs no extra service."
---

| Python | Vector search |
| --- | --- |
| `sentence_transformers.SentenceTransformer.encode` | a local embedding model |
| `numpy.linalg.norm` | normalisation |
| `matrix @ query` | dot products against every document |
| `faiss.IndexHNSWFlat` | an approximate index |
| `rank_bm25` | the keyword side of hybrid search |
