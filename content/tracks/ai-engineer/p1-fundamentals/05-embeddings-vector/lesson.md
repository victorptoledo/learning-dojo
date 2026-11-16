# Embeddings, Similarity and Vector Search

A language model's answer is only as good as the context you give it, and finding the right
context in a pile of documents is a search problem. **Embeddings** turn text into vectors so that
"similar meaning" becomes "nearby points", and **vector search** finds the nearest ones fast.

## What an embedding is

An **embedding model** maps a text (a word, a sentence, a passage) to a fixed-length vector of
numbers, for example 768 or 1,536 floats. It is trained so that texts with similar meaning get
vectors pointing in similar directions:

```text
"How do I get my money back?"   → [0.12, -0.80, 0.05, ...]
"Refund policy"                 → [0.10, -0.77, 0.09, ...]   close
"Configure the Wi-Fi router"    → [-0.65, 0.21, 0.44, ...]   far
```

Embedding models are separate from chat models: smaller, cheaper (often a few cents per million
tokens, or free to run yourself) and they produce vectors, not text. Hosted options include
OpenAI's `text-embedding-3` models, Google's Gemini embeddings, Cohere Embed and Voyage AI
(which Anthropic recommends; Anthropic does not offer its own embedding model). Open models (for
example from the sentence-transformers family) run on your own hardware.

## Similarity

The usual measure is **cosine similarity**: the cosine of the angle between two vectors, from -1
(opposite) through 0 (unrelated) to 1 (same direction).

```text
cosine(a, b) = (a · b) / (|a| × |b|)
```

If you **normalise** every vector to length 1 first, cosine similarity is just the **dot product**,
which is cheaper; many models already return unit vectors. **Euclidean distance** on unit vectors
ranks results the same way as cosine. Scores are only comparable within one model: 0.8 means
"close" for one model and "unrelated" for another, so tune thresholds per model.

## Choosing an embedding model

- **Quality on your data.** Public leaderboards (MTEB) are a start; a small test with your own
  queries decides.
- **Languages and domain.** Multilingual models put "reembolso" and "refund" close together.
- **Dimensions.** More dimensions can mean more nuance but cost storage and search time. Some
  models let you truncate vectors (Matryoshka embeddings) to trade quality for size.
- **Input limit.** Each model has a maximum input length; longer texts are cut or rejected, which is
  one reason to split documents into chunks (next kata).
- **One model per index.** Queries and documents must be embedded by the **same model and
  version**. Changing the model means re-embedding everything; store the model id with each vector.
- Some models expect a **task hint** ("query" versus "document"); use it as documented.

## Nearest-neighbour search

Given a query vector, find the k most similar stored vectors.

- **Exact (brute force)**: compare with every vector. Perfect results, linear cost. Fine up to tens
  or hundreds of thousands of vectors, especially with NumPy or a GPU.
- **Approximate nearest neighbour (ANN)**: an index that answers in roughly logarithmic time while
  missing a few true neighbours:
  - **HNSW** (hierarchical navigable small world graphs): a layered graph you walk greedily from
    coarse to fine. Fast and accurate, memory hungry. The default in most vector databases.
  - **IVF** (inverted file): cluster the vectors, search only the closest clusters. Often combined
    with **product quantization** (PQ) to compress vectors.
  ANN indexes have knobs (`ef_search`, `nprobe`) that trade recall for speed. Measure recall
  against exact search on a sample.

## Where vectors live

| Option | When |
| --- | --- |
| In memory (NumPy, FAISS) | prototypes, small static corpora, batch jobs |
| PostgreSQL with pgvector | you already run Postgres; vectors next to your rows, filters in SQL |
| Search engines (OpenSearch, Elasticsearch) | you need keyword and vector search together at scale |
| Dedicated vector databases (Qdrant, Weaviate, Milvus, Pinecone) | large or fast-changing collections, managed scaling |

Every serious store supports **metadata filters**: store `product`, `language`, `tenant_id`,
`updated_at` with each vector and filter at query time. Filters are also a **security boundary**:
a multi-tenant app must filter by tenant (or use separate collections) so one customer's documents
never reach another customer's prompt.

## Hybrid search

Embeddings capture meaning but can miss **exact tokens**: error code `E4012`, a product SKU, a
surname. Classic keyword search (**BM25**) is excellent at those. **Hybrid search** runs both and
merges the ranked lists, most simply with **reciprocal rank fusion** (RRF):

```text
score(doc) = Σ over lists  1 / (k + rank_in_list(doc))      with k ≈ 60
```

RRF needs no score calibration between the two systems, only ranks. A **reranker** (a
cross-encoder model that reads query and passage together) can then reorder the top 20–50 results
more accurately than either; you meet it in the RAG lesson.

## Measuring search quality

Collect queries with the documents that should be found, then compute **recall@k** (what fraction
of the relevant documents are in the top k) and **MRR** (mean reciprocal rank of the first relevant
result). Measure before and after every change of model, chunking or index settings.

## Summary

- Embeddings map text to vectors where similar meaning means similar direction.
- Cosine similarity, or dot product on normalised vectors; thresholds are model-specific.
- Choose the model on your own queries; embed queries and documents with the same model version.
- Exact search for small sets, ANN (HNSW, IVF) for large ones; filters by metadata, including
  tenant isolation.
- Hybrid search with BM25 and RRF covers exact terms; measure with recall@k and MRR.

## Further reading

- Reimers and Gurevych, "Sentence-BERT" (2019): <https://arxiv.org/abs/1908.10084>
- Malkov and Yashunin, "Efficient and robust approximate nearest neighbor search using HNSW graphs" (2016): <https://arxiv.org/abs/1603.09320>
- Kusupati et al., "Matryoshka Representation Learning" (2022): <https://arxiv.org/abs/2205.13147>
- Cormack, Clarke and Büttcher, "Reciprocal Rank Fusion outperforms Condorcet and individual rank learning methods" (SIGIR 2009): <https://dl.acm.org/doi/10.1145/1571941.1572114>
- OpenAI docs, embeddings: <https://platform.openai.com/docs/guides/embeddings>
- Anthropic docs, embeddings (Voyage AI): <https://platform.claude.com/docs/en/build-with-claude/embeddings>
- pgvector README (indexes, distance operators): <https://github.com/pgvector/pgvector>
- MTEB leaderboard: <https://huggingface.co/spaces/mteb/leaderboard>
